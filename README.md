Thrift OOM
=============

Introduction
============
A serious issue occured in prod env and finally it came out to be the changement of some fields in an IDL file, one of the many clients crashed due to the use of an old client version.

Root cause of this issue could be stated as：“Return value of the interface is a list, element of which is a struct and has 5 fields. A new field is added to the middle of all fields but one client still use an older version”, as shown in image below:
![]()

Even if there's a mismatch between old client and new server, the suggested "worst" condition is to just print errors, whereas crash of the client leads to a disaster. 


Reproduce OOM issue
============

This issue could be reproduced with a shorter IDL, as shown below:


You could do it yourself by downloading this project and run test case: OldClientNewServerTest.oldclient_should_oom_at_concurrency_10
In this case we use TSocket and TBinaryProtocol with thrift 0.11.0, a concurrency of 100 or even 10 could cause OOM soon.


Why there's an OOM issue
============
As we know Thrift will skip if a field type mismatches or if it's redundant, but if an exception is thrown, all subsequent methods including skip methods would be ignored.
The current request fails as we expected, but the connection will be reused by the next request, where disaster happens.
Thrift reads a TMessage object and first of all it's a readI32() which represents the length of the method's name. Unbelievable the length could be as large as 184549632, which is about 176M. And this explains why OOM occurs even at a concurrency of 10.


Why it's 184549632(176M)?
============

The way Thrift deserializes is exactly like a stack, as shown in the image below:


But as said before, thrift will validate every struct object and an exception will be thrown if it's not valid.


It's a pity that Thrift does nothing to the inputstream on such an error, and cursor still points to a middle position of inputstream, which is the position of the next element's readStructBegin, if you understand that readStructBegin() in TBinaryProtocol uses zero space, so it actually points to readFieldBegin().

If we take some time to analyze the four bytes that the readI32() method read, they would be:

byte type = TType.STRING; // byte 0：type of element Item's name field, which is TType.String with a value of 11
short id = 1;             // byte 1 and 2：seq of element Item's name field with a value of 1
int size = 7;             // byte 3：first byte of size of element Item's name field Item with a value of 6
We write a simple program to verify that the value is exactly 184549632:


There's both a certainty and uncerntainty for the number of 184549632, it would be different if the next field is not a String. 

Another IDL file named sample2 in this project will give another number 83888648M, which is about 8M and will not cause OOM.

Lucky enough? No, the client side will read data of 8388864 bytes from inputstream, and of course the connection is blocked.


How to fix it
============
If there's a mismatch of IDL files between clients and server, we prefer the way to print errors instead of OOM.

There are several methods to do this but the method below should be a simple and elegant one:

1. Provide a safeClear() method with the default behavior to do nothing in TTransport
2. Clear inputstream in TSocket, and change a little in several other classes.
3. Call safeClear() in TServiceClient

I did a load test on my Macbook Air, which has 100 concurrencies and last 15 minutes with a total request number of 160 millions and an average data size of 0.5K, both client and server sides work fine.


Any quick workarounds?
============
Method 1: strictread mode

Thrift众多的配置项中，有严格读（strictRead）、严格写(strictWrite)两个选项，由于严格读默认值为False，改为true是否可以呢？如果OK的话，只需要修改配置而无需修改源码，这将会是最轻量级的方案。查看Thrift源码，如果命中严格读会提前报错，避免下方readStringBody()方法分配太大的内存空间：



严格读（strictRead）的修改方式如下：

// 很多情况下大家如此使用TProtocol创建连接，此时strictWrite值为true，而、strictRead值为false；
TProtocol protocol = new TBinaryProtocol(transport);
// 严格读：直接创建TBinaryProtocol，传入参数
TProtocol protocol = new TBinaryProtocol(transport, true, true);
// 严格读：使用Protocol工厂模式，传入参数
TBinaryProtocol.Factory protFactory = new TBinaryProtocol.Factory(true, true);
经测试验证，使用严格读之后，客户端还会报错，但OOM问题消失了！“貌似”这也是我们想要的结果，压测效果如何呢？

在Macbook Air上，使用100个并发进行验证，5分钟之后，发送大约56万请求，客户端出现大量的SocketException(Broken pipe)，甚至客户端开始宕死。

究其原因，strictRead虽然避免了创建大量字节数组，但抛异常时thrift也未对输入流做任何清理，会产生误读取残余数据，甚至引起连接阻塞。

所以使用严格读，并不能解决这一问题，同样thrift提供了另外一个配置项“读取字符串或字节的最大长度（stringLengthLimit_）”，也不能解决该问题。

思路二：使用短连接

改用短连接，发送每个请求都创建一个连接，这样可以避免请求之间的数据污染，但毫无疑问也会带来较大的性能损失，通常不建议。

思路三：使用TFramedTransport

TFramedTransport对整帧数据使用了缓存，一次性把底层Socket中Inputstream中的数据完全读到了readBuffer，理论上不会出现OOM了。

验证了一把，很可惜还是会OOM，原因是抛异常之后，TFramedTransport中的readBuffer中的数据未做清理，下次请求会错误读取readBuffer中的残余数据。

如果你想验证，可以运行OldClientNewServerTest中的测试用例：oldclient_should_oom_if_use_TFramedTransport_at_concurrency_10

小结：使用严格读、限制字符串长度等配置方式，使用TFramedTransport都不能完美解决问题，要么OOM，要么连接被阻塞。而短连接对性能影响较大，在Thrift中也很少使用。

A summary
============

1. There's a chance of unexpected consequences if IDL versions of client and server mismatch, it could happen to both client and server, the consequence could be: misalignment of fields、connections being blocked, or even OOM.   

2. This bug exists in a chain of thrift versions from 0.8(or maybe earlier) up to the latest 0.13.0-snapshot

4. No quick workarounds available：strictRead mode or restrictStringLength does not work; TFramedTransport does work either unless manually call clear() method to clear readBuff. The best way is to remember: Keep IDL files up to dates between clients and servers

5. The proposed method：make sure to clean residual bytes in TSocket's inputstream or TFramedTransport's buffread, a load test of 100 concurrencies has proven it
