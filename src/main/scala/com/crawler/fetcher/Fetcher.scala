package com.crawler.fetcher

import com.crawler.Logging
import play.api.libs.ws.StandaloneWSClient

import scala.concurrent.{ExecutionContext, Future}

case class ContentBody(html: String) extends AnyVal

case class FetchFailure(msg: String) extends RuntimeException(msg)

trait Fetcher {
  def get(url: String)(implicit ec: ExecutionContext): Future[Option[ContentBody]]
}

class PlayWsHttpFetcher(client: StandaloneWSClient, host: String) extends Fetcher with Logging {

  override def get(url: String)(implicit ec: ExecutionContext): Future[Option[ContentBody]] = {
    client.url(s"$host$url").withFollowRedirects(true).get().map { response =>
      response.status match {
        case 200 => Some(ContentBody(response.body))
        case 404 =>
          log(s"skipped $url, statuscode=404")
          None
        case _ => throw FetchFailure(s"Failed to fetch $url, statuscode=${response.status}")
      }
    }
  }
}