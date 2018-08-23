package com.crawler.parser

import com.crawler.fetcher.ContentBody
import org.jsoup.Jsoup

import scala.collection.JavaConversions._

case class WebPage(urls: List[String])

case class ParseFailure(msg: String) extends RuntimeException(msg)

trait Parser {
  def parsePageContent(body: ContentBody): WebPage
}

class JsoupParser extends Parser {

  override def parsePageContent(body: ContentBody): WebPage = {
    val doc = Jsoup.parse(body.html)
    val anchors = doc.getElementsByTag("a").toList
    val urls = anchors.map(_.attr("href"))
      .filter(str => str.startsWith("/"))
    WebPage(urls)
  }

}