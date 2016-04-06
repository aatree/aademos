# w3c-worker demo

This project is an adoption of the [w3c schools webworker example][1].

```
│   .gitignore
│   boot.properties
│   build.boot
│   project.clj
│   README.md
│
└───src
    │   worker.cljs.edn
    │
    ├───count
    │       index.cljs.hl
    │
    └───counter_worker
            counts.cljs
```

##Videos

- [boot.properties file](http://aatree.github.io/videos/boot.properties.html)
- [project.clj file generation](http://aatree.github.io/videos/project.clj.html)
- [Demo](http://aatree.github.io/videos/w3c-worker.html)

## Usage

1. Start the auto-compiler with audio feedback. In a terminal:

    ```bash
    $ boot dev
    ```

2. Go to [http://localhost:9000][3] in your browser.

[1]: http://www.w3schools.com/html/html5_webworkers.asp
[2]: https://hoplon.io
[3]: http://localhost:9000

