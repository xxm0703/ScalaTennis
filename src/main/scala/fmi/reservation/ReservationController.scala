package fmi.reservation

import cats.effect.IO
import cats.syntax.all.*
import fmi.{ConflictDescription, ResourceNotFound}
import fmi.club.CourtId
import fmi.user.UserId
import fmi.user.authentication.{AuthenticatedUser, AuthenticationService}
import fmi.utils.CirceUtils
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.http4s.AuthedRoutes
import org.http4s.dsl.io.*
import sttp.tapir.server.ServerEndpoint

class ReservationController(reservationService: ReservationService)(authenticationService: AuthenticationService):
  import authenticationService.authenticate

  private val placeReservation = ReservationEndpoints.placeReservationEndpoint
    .authenticate()
    .serverLogic { user => reservationForm =>
      reservationService
        .placeReservation(user.id, reservationForm)
        .map(_.leftMap(_ => ConflictDescription("Slot is already taken")))
    }

  private val getAllReservationsPerCourt = ReservationEndpoints.getAllReservationsForCourtEndpoint
    .authenticate()
    .serverLogic { user => courtId =>
      reservationService
        .getAllReservationsForCourt(courtId)
        .map(_.asRight)
    }

  private val deleteReservation = ReservationEndpoints.deleteReservationEndpoint
    .authenticate()
    .serverLogic { user => reservationId =>
      reservationService
        .deleteReservationLogic(reservationId)
        .map(_.leftMap(_ => ResourceNotFound("No reservation with given id was found")))
    }

  val endpoints: List[ServerEndpoint[Any, IO]] = List(
    placeReservation,
    getAllReservationsPerCourt,
    deleteReservation
  )
