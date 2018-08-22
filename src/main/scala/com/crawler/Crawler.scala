package com.crawler

import com.crawler.fetcher.Fetcher
import com.crawler.parser.{Parser, WebPage}

import scala.annotation.tailrec
import scala.collection.immutable.HashSet
import scala.concurrent.{ExecutionContext, Future}

trait Logging {
  def log(msg: String) = println(msg)
}

trait Crawler {
  def crawl(webUrl: String)(implicit ec: ExecutionContext): Future[Set[String]]
}

class SimpleCrawler(fetcher: Fetcher, parser: Parser) extends Crawler with Logging {

  def crawl(webUrl: String)(implicit ec: ExecutionContext): Future[Set[String]] = {

    def _crawl(currentUrls: List[String], sitemap: HashSet[String]): Future[Set[String]] = {

      log(s"crawl urls=${currentUrls.mkString(" ")}, \n total=${currentUrls.size}")

      val nextUrlsFuture = for {
        contentBodies <- Future.sequence(currentUrls.map(fetcher.get))
        parsedPagesWithUrls <- Future.sequence(contentBodies.flatMap(_.map(parser.parsePageContent)))
        nextUrls = extractNonVisitedUrls(parsedPagesWithUrls, sitemap)
      } yield nextUrls

      val updatedSitemap = sitemap ++ currentUrls.toSet

      nextUrlsFuture.flatMap {
        case urls if urls.nonEmpty => _crawl(urls, updatedSitemap)
        case Nil => Future.successful(updatedSitemap)
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
