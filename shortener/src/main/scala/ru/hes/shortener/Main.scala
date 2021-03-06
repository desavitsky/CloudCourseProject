package ru.hes.shortener

import cats.effect.kernel.Resource
import cats.effect.{ExitCode, IO, IOApp}
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.effect.Log.Stdout.instance
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import ru.hes.shortener.api.ShortenerRoutes
import ru.hes.shortener.service.{AuthServiceImpl, KeyGeneratorImpl, ShortenerServiceImpl}
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

object Main extends IOApp.Simple {

  lazy val service: Resource[IO, ShortenerRoutes] = for {
    redis <- Redis[IO].utf8("redis://:letsrediswithme1@10.20.14.30:6379")
    sttp <- AsyncHttpClientCatsBackend.resource[IO]()
    authService = new AuthServiceImpl[IO](sttp)
    keyGenerator = new KeyGeneratorImpl
    shortener = new ShortenerServiceImpl(keyGenerator, redis)
  } yield new ShortenerRoutes(shortener, authService)

  override def run: IO[Unit] = {
    service.use { router =>
      BlazeServerBuilder[IO]
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(Router("/" -> router.routes).orNotFound)
        .resource
        .useForever
        .as(ExitCode.Success)
    }
  }
}
