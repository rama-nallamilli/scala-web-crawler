package com.crawler

import com.crawler.fetcher.{ContentBody, FetchFailure, Fetcher}
import com.crawler.parser.{ParseFailure, Parser, WebPage}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.{ExecutionContext, Future}

class CrawlerSpec extends FlatSpec with ScalaFutures with Matchers {

  private val mockParser: Parser = (body: ContentBody) => {
    WebPage(urls = body.html.split(",").filter(_.nonEmpty).toList)
  }

  private val mockFetcher: Fetcher = new Fetcher {
    override def get(url: String)(implicit ec: ExecutionContext): Future[Option[ContentBody]] = Future {
      val result = url match {
        case "/home" => ContentBody(html = "/about,/careers")
        case "/about" => ContentBody(html = "/about/address")
        case "/careers" => ContentBody(html = "/careers/jobs")
        case "/careers/jobs" => ContentBody(html = "/careers/jobs/london")
        case _ => ContentBody(html = "")
      }

      Thread.sleep(1000)
      Some(result)
    }
  }

  "crawl" should "crawl all pages" in {

    val crawler = new SimpleCrawler(mockFetcher, mockParser)
    val sitemap = timed {
      crawler.crawl("/home")
    }

    sitemap shouldBe Set("/about",
      "/about/address",
      "/careers",
      "/careers/jobs",
      "/careers/jobs/london",
      "/home")
  }

  it should "crawl pages when there is a circular reference" in {

    val fetcherWithCircularRef: Fetcher = new Fetcher {
      override def get(url: String)(
        implicit ec: ExecutionContext): Future[Option[ContentBody]] = {
        val result = url match {
          case "/home" => ContentBody(html = "/about,/careers")
          case "/about" => ContentBody(html = "/about/address")
          case "/about/address" => ContentBody(html = "/home")
          case _ => ContentBody(html = "")
        }
        Future.successful(Some(result))
      }
    }

    val crawler = new SimpleCrawler(fetcherWithCircularRef, mockParser)
    val sitemap = crawler.crawl("/home")

    sitemap shouldBe Set("/about",
      "/about/address",
      "/careers",
      "/home")
  }

  it should "fail fast if there is a parsing error" in {

    val failingParser: Parser = (body: ContentBody) => throw ParseFailure("parsing failure!")

    val crawler = new SimpleCrawler(mockFetcher, failingParser)

    assertThrows[ParseFailure] {
      crawler.crawl("/home")
    }
  }

  it should "fail fast if there is a fetching error" in {

    val failingFetcher: Fetcher = new Fetcher {
      override def get(url: String)(
        implicit ec: ExecutionContext): Future[Option[ContentBody]] = {
        val result = url match {
          case "/home" => ContentBody(html = "/about,/careers")
          case "/about" => ContentBody(html = "/about/address")
          case "/careers" =>
            throw FetchFailure("Failed to fetch /careers, statuscode=500")
          case _ => ContentBody(html = "")
        }
        Future.successful(Some(result))
      }
    }

    val crawler = new SimpleCrawler(failingFetcher, mockParser)

    assertThrows[FetchFailure] {
      crawler.crawl("/home")
    }
  }

  private def timed[T](fn: => T) = {
    val start = System.currentTimeMillis()
    val result = fn
    println(s"time taken = ${System.currentTimeMillis() - start}ms")
    result
  }

}
