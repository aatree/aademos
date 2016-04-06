# duracell demo

This is a demo showing how easy it is to use 
[IndexedDb](https://developer.mozilla.org/en-US/docs/Web/API/IndexedDB_API)
running in a
[web worker](http://www.w3schools.com/html/html5_webworkers.asp).

```
│   .gitignore
│   boot.properties
│   build.boot
│   project.clj
│   README.md
│
└───src
    └───client
        └───duracell
                index.cljs.hl
                strap.clj
```

##Videos

- [boot.properties file](http://aatree.github.io/videos/boot.properties.html)
- [Demo](http://aatree.github.io/videos/duracell.html)

The duracell demo builds on hoplon,
[aaworker](https://github.com/aatree/aaworker)
and [durable-cells](https://github.com/aatree/durable-cells).
