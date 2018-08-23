package com.crawler.parser

import com.crawler.fetcher.ContentBody
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}

class ParserSpec extends FlatSpec with ScalaFutures with Matchers {

  "Jsoup Parser" should "extract urls" in {

    val body =
      ContentBody(
        """
          |<html>
          |<head>Hello world!</head>
          |<body>
          |  <a href="/about" />
          |  <a href="/blog" />
          |</body>
          |</html>
        """.stripMargin)

    val parser = new JsoupParser()
    parser.parsePageContent(body) shouldBe WebPage(List("/about", "/blog"))
  }

  it should "ignore urls to external websites" in {
    val body =
      ContentBody(
        """
          |<html>
          |<head>Hello world!</head>
          |<body>
          |  <a href="/about" />
          |  <a href="/blog" />
          |  <a href="https://www.facebook.com" />
          |</body>
          |</html>
        """.stripMargin)

    val parser = new JsoupParser()
    parser.parsePageContent(body) shouldBe WebPage(List("/about", "/blog"))
  }

  it should "return no urls for malformed content" in {
    val parser = new JsoupParser()
    parser.parsePageContent(ContentBody("%$£@")) shouldBe WebPage(urls = Nil)
  }
}
