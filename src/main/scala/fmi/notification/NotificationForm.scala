package fmi.notification

import fmi.court.CourtId
import fmi.club.ClubId
import fmi.user.UserId
import fmi.reservation.ReservationId
import io.circe.Codec
import sttp.tapir.Schema

import java.time.Instant

case class NotificationForm(
  id: NotificationId,
  notificationType: NotificationType,
  triggeredBy: UserId,
  clubId: Option[ClubId],
  courtId: Option[CourtId],
  reservationId: Option[ReservationId],
  status: NotificationStatus
) derives Codec,
      Schema
