package com.crawler.parser

import com.crawler.fetcher.ContentBody
import org.jsoup.Jsoup

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}

case class WebPage(urls: List[String])

case class ParseFailure(msg: String) extends RuntimeException(msg)

trait Parser {
  def parsePageContent(body: ContentBody)(implicit ec: ExecutionContext): Future[WebPage]
}

class JsoupParser extends Parser {

  override def parsePageContent(body: ContentBody)(implicit ec: ExecutionContext): Future[WebPage] = {
    Future {
      val doc = Jsoup.parse(body.html)
      val anchors = doc.getElementsByTag("a").toList
      val urls = anchors.map(_.attr("href"))
        .filter(str => str.startsWith("/") && !str.startsWith("/cdn-cgi"))
      WebPage(urls)
    }
  }

}