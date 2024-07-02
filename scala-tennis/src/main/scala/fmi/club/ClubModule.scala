package fmi.club

import cats.effect.IO
import cats.effect.kernel.Resource
import fmi.infrastructure.db.DoobieDatabase.DbTransactor
import fmi.user.authentication.{AuthenticatedUser, AuthenticationService}
import org.http4s.{AuthedRoutes, HttpRoutes}
import sttp.tapir.server.ServerEndpoint

case class ClubModule(
  courtDao: CourtDao,
  courtAvailabilityDao: CourtAvailabilityDao,
  endpoints: List[ServerEndpoint[Any, IO]]
)

object ClubModule:
  def apply(dbTransactor: DbTransactor, authenticationService: AuthenticationService): Resource[IO, ClubModule] =
    val courtDao = new CourtDao(dbTransactor)
    val courtAvailabilityDao = new CourtAvailabilityDao(dbTransactor)
    val clubController = new ClubController(courtDao, courtAvailabilityDao)(authenticationService)

    Resource.pure(
      ClubModule(
        courtDao,
        courtAvailabilityDao,
        clubController.endpoints
      )
    )
