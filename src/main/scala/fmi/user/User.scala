package fmi.user

import doobie.Meta
import fmi.utils.CirceUtils
import io.circe.Codec
import io.circe.derivation.{Configuration, ConfiguredEnumCodec}
import sttp.tapir.{Schema, SchemaType, CodecFormat}
import fmi.utils.DerivationConfiguration.given Configuration
import sttp.tapir

case class User(
  id: UserId,
  passwordHash: String,
  role: UserRole,
  name: String,
  age: Option[Int]
)

case class UserId(email: String)

object UserId:
  given Codec[UserId] = CirceUtils.unwrappedCodec(UserId.apply)(_.email)
  given Schema[UserId] = Schema(SchemaType.SString())
  given tapir.Codec[String, UserId, CodecFormat.TextPlain] = tapir.Codec.string.map(UserId.apply)(_.email)

enum UserRole derives ConfiguredEnumCodec:
  case Admin, Owner, Player

object UserRole:
  given Meta[UserRole] = Meta[String].imap[UserRole](x => UserRole.valueOf(x.capitalize))(_.toString.toLowerCase)
  given Schema[UserRole] = Schema.derivedEnumeration()
