package com.dki.meeting.identity.application.port.in;

import com.dki.meeting.foundation.*;
import com.dki.meeting.identity.application.model.*;
import com.dki.meeting.identity.application.model.IdentityModels.*;
import java.util.*;

public record IdentityHandlers(
    UseCase<IdentityCommands.Login, Tokens> login,
    UseCase<IdentityCommands.Refresh, Tokens> refresh,
    UseCase<IdentityCommands.Refresh, Void> logout,
    UseCase<Void, UserView> me,
    UseCase<IdentityCommands.Page, PageResult<UserView>> users,
    UseCase<IdentityCommands.Directory, PageResult<UserView>> directory,
    UseCase<IdentityCommands.Create, UserView> create,
    UseCase<IdentityCommands.Update, UserView> update,
    UseCase<IdentityCommands.PasswordChange, Void> changePassword,
    UseCase<IdentityCommands.Bootstrap, Void> bootstrap) {}
