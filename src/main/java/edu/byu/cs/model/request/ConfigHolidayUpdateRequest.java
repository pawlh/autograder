package edu.byu.cs.model.request;

import java.util.List;

public record ConfigHolidayUpdateRequest(
        List<String> holidays
) {}
