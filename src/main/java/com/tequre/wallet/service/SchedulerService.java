package com.tequre.wallet.service;

import com.tequre.wallet.data.Agent;
import com.tequre.wallet.enums.SubAgentType;
import com.tequre.wallet.enums.SyncStatus;
import com.tequre.wallet.repository.AgentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SchedulerService {

    private final Logger logger = LoggerFactory.getLogger(SchedulerService.class);

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private UrbanMeterReaderService urbanMeterReaderService;

    @Autowired
    private RuralMeterReaderService ruralMeterReaderService;

    @Scheduled(cron = "${scheduler.meterAgentSync}", zone = "Asia/Kolkata")
    public void syncMeterAgents() {
        logger.info("Sync Job for Meter Agents - Started " + new Date());
        Agent queryAgent = new Agent();
        queryAgent.setSubAgentType(SubAgentType.METER_READER_AGENT);
        queryAgent.setSyncStatus(SyncStatus.SYNC_PENDING);
        List<Agent> synchableAgents = agentRepository.findAll(Example.of(queryAgent));
        logger.info("Synchable Agents : " + synchableAgents.size());
        Set<String> agencyIds = synchableAgents.parallelStream()
                .map(agent -> agent.getAgencyId()).collect(Collectors.toSet());
        Iterable<Agent> agencyIterator = agentRepository.findAllById(agencyIds);
        Map<String, Agent> agencyMap = new HashMap<>();
        agencyIterator.forEach(i -> agencyMap.put(i.getId(), i));
        synchableAgents.forEach(agent -> {
            Agent agency = agencyMap.get(agent.getAgencyId());
            if (agency == null) {
                logger.warn("No agency found for agent id " + agent.getId());
            } else {
                boolean isSynched = false;
                switch (agent.getAreaType()) {
                    case NON_RAPDRP:
                        isSynched = ruralMeterReaderService.sync(agent.getUniqueId(),
                                agent.getDiscoms() != null && agent.getDiscoms().size() > 1 ? agent.getDiscoms().get(0) : "",
                                agent.getVan(), agency.getId());
                        break;
                    case RAPDRP:
                        isSynched = urbanMeterReaderService.sync(agent.getUniqueId(),
                                agency.getId(), agency.getVan(), agent.getId(), agent.getVan(),agent.getDiscoms() != null && agent.getDiscoms().size() > 1 ? agent.getDiscoms().get(0) : "");
                        break;
                    default:
                        logger.warn("Invalid area type " + agent.getAreaType());
                }
                if (isSynched) {
                    agent.setSyncStatus(SyncStatus.SYNC_COMPLETED);
                    agentRepository.save(agent);
                }
            }
        });
        logger.info("Sync Job for Meter Agents - Completed " + new Date());
    }
}
