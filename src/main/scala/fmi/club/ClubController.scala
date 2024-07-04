package fmi.club
import cats.effect.IO
import cats.syntax.all.*
import fmi.user.authentication.AuthenticationService
import fmi.{ConflictDescription, ResourceNotFound}
import sttp.tapir.server.ServerEndpoint

class ClubController(
  courtDao: CourtDao,
  courtAvailabilityDao: CourtAvailabilityDao
)(
  authenticationService: AuthenticationService
):
  import authenticationService.authenticateAdmin

  val getCourt = ClubEndpoints.getCourtEndpoint.serverLogic: courtId =>
    courtDao
      .retrieveCourt(courtId)
      .map(_.toRight(ResourceNotFound(s"Court $courtId was not found")))

  val putCourt = ClubEndpoints.putCourtEndpoint.authenticateAdmin
    .serverLogicSuccess(user => courtDao.addCourt)

  val getAllAvailability = ClubEndpoints.getAllAvailabilityEndpoint.serverLogicSuccess: _ =>
    courtAvailabilityDao.retrieveAllAvailableAvailability

  val adjustAvailability = ClubEndpoints.adjustAvailabilityEndpoint.authenticateAdmin
    .serverLogic { user => adjustment =>
      courtAvailabilityDao
        .applyClubAdjustment(adjustment)
        .map:
          case SuccessfulAdjustment => ().asRight
          case NotEnoughAvailabilityAvailable => ConflictDescription("Not enough availability available").asLeft
    }

  val endpoints: List[ServerEndpoint[Any, IO]] = List(
    getCourt,
    putCourt,
    getAllAvailability,
    adjustAvailability
  )
