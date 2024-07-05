package fmi.reservation

import cats.effect.IO
import cats.syntax.all.*
import fmi.{
  ConflictDescription,
  OperationNotAllowed,
  ReservationDeletionError,
  ReservationStatusUpdateError,
  ResourceNotFound
}
import fmi.club.CourtId
import fmi.user.{UserId, UserRole}
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

  // TODO Only admin, club owner or user who created the reservation can delete it
  private val deleteReservation = ReservationEndpoints.deleteReservationEndpoint
    .authenticate()
    .serverLogic { user => reservationId =>
      if user.role.equals(UserRole.Admin) then
        reservationService
          .deleteReservationLogic(reservationId)
          .map(_.leftMap(_ => ReservationDeletionError("Reservation could not be deleted")))
      else IO.pure(Left(ReservationDeletionError("User is not authorized to delete reservations")))
    }

  // TODO only allow this if the user is an admin, the owner or the user who made the reservation
  private val updateReservationStatus = ReservationEndpoints.changeReservationStatusEndpoint
    .authenticate()
    .serverLogic { user => reservationStatusChangeForm =>
      if user.role.equals(UserRole.Admin) then
        reservationService
          .updateReservationStatus(reservationStatusChangeForm)
          .map(_.leftMap(_ => ReservationStatusUpdateError("Reservation status could npt be changed")))
      else IO.pure(Left(ReservationStatusUpdateError("User is not authorized to change reservation status")))
    }
  
  val endpoints: List[ServerEndpoint[Any, IO]] = List(
    placeReservation,
    getAllReservationsPerCourt,
    deleteReservation,
    updateReservationStatus
  )
