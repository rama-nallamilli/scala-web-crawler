# Sitemap Web Crawler

A simple web crawler written in Scala.  Outputs a sitemap of the website excluding external links.
 
## Dependencies
* Jsoup for Parsing HTML urls - https://jsoup.org/
* Play WS for async REST calls - https://github.com/playframework/play-ws

## Compile and run tests
```
sbt clean test
```

## Build and run in docker container
```
sbt docker:publishLocal
docker run --rm web-crawler:0.1 https://monzo.com
```