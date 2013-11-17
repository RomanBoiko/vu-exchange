vu-exchange
===========

Exchange prototype (Smarkets)

Running
-------

* test: mvn test
* run: mvn install; ./target/exchange env/boikoro/app.properties

[events input(via tcp) -> events persisting(flat file)]{multiple workers} -> [disruptor] -> [business processor]{single thread} -> disruptor -> [output events publishing]{multiple workers}