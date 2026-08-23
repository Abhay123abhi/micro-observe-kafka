package com.microobserve.incident.model;

import com.microobserve.incident.api.AlertmanagerWebhook;

public record InvestigationRequested(String incidentId, AlertmanagerWebhook.Alert alert) {
}
