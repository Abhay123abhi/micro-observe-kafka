package com.microobserve.incident.service;

import com.microobserve.incident.model.IncidentAnalysis;
import com.microobserve.incident.model.IncidentEvidence;

public interface IncidentInvestigator {

    IncidentAnalysis investigate(IncidentEvidence evidence);
}
