package com.crawler

import com.crawler.fetcher.{ContentBody, Fetcher}
import com.crawler.parser.{Parser, WebPage}

import scala.annotation.tailrec
import scala.collection.immutable.HashSet
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

trait Logging {
  def log(msg: String) = println(msg)
}

trait Crawler {
  def crawl(webUrl: String): Set[String]
}

class SimpleCrawler(fetcher: Fetcher, parser: Parser) extends Crawler with Logging {

  /*

  Scala Exercise
  **************

  The current crawler implementation is quite slow as it synchronously fetches
  each url per iteration, before moving onto the next one.

  Update the method to return a future as follows:

    def _crawl(currentUrls: List[String], sitemap: HashSet[String]): Future[Set[String]]

  For each level fetch pages concurrently

  Tips:
    1. Future.sequence can transform a List[Future[A]] => Future[List[A]]
    2. extractNonVisitedUrls may be useful to filter urls from fetched pages
  */


  def crawl(webUrl: String): Set[String] = {

    @tailrec
    def _crawl(currentUrls: List[String], sitemap: HashSet[String]): Set[String] = {

      log(s"crawl urls=${currentUrls.mkString(" ")}, \n total=${currentUrls.size}")

      currentUrls match {
        case head :: tail =>
          val content: Option[ContentBody] = Await.result(fetcher.get(head), 2.seconds)
          val nextUrls = content
            .map(body => parser.parsePageContent(body).urls)
            .getOrElse(Nil)
            .filterNot(sitemap.contains)

          _crawl(tail ++ nextUrls, sitemap ++ currentUrls.toSet)
        case Nil => sitemap
      }

    }

    _crawl(List(webUrl), HashSet.empty[String])
  }

  private def extractNonVisitedUrls(pages: List[WebPage], visited: HashSet[String]): List[String] =
    pages
      .flatMap(_.urls)
      .distinct
      .filterNot(visited.contains)

}
