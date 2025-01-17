package fmi.user

import cats.effect.IO
import cats.syntax.all.*
import fmi.UnauthorizedAccess
import fmi.user.authentication.{AuthenticatedUser, AuthenticationService}
import sttp.tapir.server.ServerEndpoint

class UsersController(
  usersService: UsersService
)(
  authenticationService: AuthenticationService
):
  import authenticationService.authenticate

  val endpoints: List[ServerEndpoint[Any, IO]] = List(
    registerUser,
    getAuthenticatedUser,
    loginUser,
    logoutUser
  )

  def registerUser = UsersEndpoints.registerUserEndpoint.serverLogic: userRegistrationForm =>
    usersService.registerUser(userRegistrationForm).map(_.void)

  def getAuthenticatedUser = UsersEndpoints.getAuthenticatedUserEndpoint
    .authenticate()
    .serverLogicSuccess(user => _ => user.pure[IO])

  def loginUser = UsersEndpoints.loginUserEndpoint.serverLogic: userLogin =>
    usersService
      .login(userLogin)
      .flatMap:
        case Some(user) => authenticationService.sessionWithUser(user.id).map(_.asRight)
        case None => UnauthorizedAccess("Invalid credentials").asLeft.pure[IO]

  def logoutUser =
    UsersEndpoints.logoutUserEndpoint.serverLogicSuccess(_ => authenticationService.clearSession)
