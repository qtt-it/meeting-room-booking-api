package com.dki.meeting.room.application.port.in;

import com.dki.meeting.foundation.*;
import com.dki.meeting.room.application.RoomView;
import com.dki.meeting.room.application.model.*;
import java.util.*;

public record RoomHandlers(
    UseCase<RoomCommands.Catalog, PageResult<RoomView>> catalog,
    UseCase<UUID, RoomView> get,
    UseCase<RoomCommands.Input, RoomView> create,
    UseCase<RoomCommands.Update, RoomView> update) {}
