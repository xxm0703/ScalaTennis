package fmi.club

import cats.effect.IO
import cats.effect.kernel.Resource
import fmi.infrastructure.db.DoobieDatabase.DbTransactor
import fmi.user.authentication.AuthenticationService
import sttp.tapir.server.ServerEndpoint

case class ClubModule(
  courtDao: CourtDao,
  endpoints: List[ServerEndpoint[Any, IO]]
)

object ClubModule:
  def apply(dbTransactor: DbTransactor, authenticationService: AuthenticationService): Resource[IO, ClubModule] =
    val courtDao = new CourtDao(dbTransactor)
    val clubController = new ClubController(courtDao)(authenticationService)

    Resource.pure(
      ClubModule(
        courtDao,
        clubController.endpoints
      )
    )
