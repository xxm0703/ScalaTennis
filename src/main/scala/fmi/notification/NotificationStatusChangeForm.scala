package fmi.notification

import io.circe.Codec
import sttp.tapir.Schema

case class NotificationStatusChangeForm(notificationId: NotificationId, notificationStatus: NotificationStatus)
    derives Codec,
      Schema
