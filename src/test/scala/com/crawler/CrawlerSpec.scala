package com.crawler

import com.crawler.fetcher.{ContentBody, FetchFailure, Fetcher}
import com.crawler.parser.{ParseFailure, Parser, WebPage}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.{ExecutionContext, Future}

class CrawlerSpec extends FlatSpec with ScalaFutures with Matchers {

  private val mockParser: Parser = new Parser {
    override def parsePageContent(body: ContentBody)(
      implicit ec: ExecutionContext): Future[WebPage] = {
      Future.successful(
        WebPage(urls = body.html.split(",").filter(_.nonEmpty).toList))
    }
  }

  private val mockFetcher: Fetcher = new Fetcher {
    override def get(url: String)(
      implicit ec: ExecutionContext): Future[Option[ContentBody]] = {
      val result = url match {
        case "/home" => ContentBody(html = "/about,/careers")
        case "/about" => ContentBody(html = "/about/address")
        case "/careers" => ContentBody(html = "/careers/jobs")
        case "/careers/jobs" => ContentBody(html = "/careers/jobs/london")
        case _ => ContentBody(html = "")
      }

      Future.successful(Some(result))
    }
  }

  "crawl" should "crawl all pages" in {

    val crawler = new SimpleCrawler(mockFetcher, mockParser)
    val sitemap = crawler.crawl("/home")

    sitemap.futureValue shouldBe Set("/about",
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

    sitemap.futureValue shouldBe Set("/about",
      "/about/address",
      "/careers",
      "/home")
  }

  it should "fail fast if there is a parsing error" in {

    val failingParser: Parser = new Parser {
      override def parsePageContent(body: ContentBody)(
        implicit ec: ExecutionContext): Future[WebPage] =
        Future.failed(ParseFailure("parsing failure!"))
    }

    val crawler = new SimpleCrawler(mockFetcher, failingParser)

    val sitemap = crawler.crawl("/home")
    sitemap.failed.futureValue shouldBe ParseFailure("parsing failure!")
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

    val sitemap = crawler.crawl("/home")
    sitemap.failed.futureValue shouldBe FetchFailure(
      "Failed to fetch /careers, statuscode=500")
  }
}
