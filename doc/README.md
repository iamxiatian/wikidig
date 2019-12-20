# 设计文档

## HTTP服务

HTTP服务的入口为HttpServer类，该类会加载各个Restful API的处理类，
API的处理类以Route结尾，如关键词抽取为KeywordRoute.scala文件。

HttpServer服务采用了spark java实现，默认会优先响应API的拦截处理，
没有对应的API时，则会读取www目录下的文件。


subgraph:

抽取出的一个子图示例，采用dot语法表示父子关系，并利用dot转换为png图片，方便观察

