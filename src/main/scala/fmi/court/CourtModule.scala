package fmi.court

import cats.effect.IO
import cats.effect.kernel.Resource
import fmi.court.CourtDao
import fmi.infrastructure.db.DoobieDatabase.DbTransactor
import fmi.user.authentication.AuthenticationService
import sttp.tapir.server.ServerEndpoint

case class CourtModule(
  courtDao: CourtDao,
  endpoints: List[ServerEndpoint[Any, IO]]
)

object CourtModule:
  def apply(dbTransactor: DbTransactor, authenticationService: AuthenticationService): Resource[IO, CourtModule] =
    val courtDao = new CourtDao(dbTransactor)
    val courtController = new CourtController(courtDao)(authenticationService)

    Resource.pure(
      CourtModule(
        courtDao,
        courtController.endpoints
      )
    )
