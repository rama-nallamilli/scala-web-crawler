package com.crawler

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.crawler.fetcher.PlayWsHttpFetcher
import com.crawler.parser.JsoupParser
import play.api.libs.ws.ahc.StandaloneAhcWSClient

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try
import scala.concurrent.ExecutionContext.Implicits.global

object Main extends App with Logging {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val client = StandaloneAhcWSClient()

  val host = Try(args(0)).getOrElse("https://monzo.com")
  val fetcher = new PlayWsHttpFetcher(client, host)
  val parser = new JsoupParser
  val crawler = new SimpleCrawler(fetcher, parser)

  val start = System.currentTimeMillis()
  val result = Await.result(crawler.crawl("/"), 2.minutes)
  log(s"\n$host\n\n${result.toList.sorted.mkString("\n")}")
  log(s"\n Completed in ${System.currentTimeMillis() - start} ms")
}
