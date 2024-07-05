package fmi.club

import cats.effect.IO
import cats.effect.kernel.Resource
import fmi.infrastructure.db.DoobieDatabase.DbTransactor
import fmi.user.authentication.AuthenticationService
import sttp.tapir.server.ServerEndpoint

case class ClubModule(
  clubDao: ClubDao,
  endpoints: List[ServerEndpoint[Any, IO]]
)

object ClubModule:
  def apply(dbTransactor: DbTransactor, authenticationService: AuthenticationService): Resource[IO, ClubModule] =
    val clubDao = new ClubDao(dbTransactor)
    val clubController = new ClubController(clubDao)(authenticationService)

    Resource.pure(
      ClubModule(
        clubDao,
        clubController.endpoints
      )
    )
