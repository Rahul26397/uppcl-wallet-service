package com.tequre.wallet.controller;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.Gson;
import com.mongodb.MongoClient;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Projections;
import com.tequre.wallet.data.Agent;
import com.tequre.wallet.data.ServiceMessage;
import com.tequre.wallet.data.Transaction;
import com.tequre.wallet.data.User;
import com.tequre.wallet.data.Wallet;
import com.tequre.wallet.enums.AgentType;
import com.tequre.wallet.enums.PaymentType;
import com.tequre.wallet.enums.SourceType;
import com.tequre.wallet.enums.TransactionType;
import com.tequre.wallet.repository.AgentRepository;
import com.tequre.wallet.repository.WalletRepository;
import com.tequre.wallet.request.Page;
import com.tequre.wallet.response.RuralUrbanMappingResponse;
import com.tequre.wallet.response.report.AgencyAgentAnalyticsRecord;
import com.tequre.wallet.response.report.AgencyAgents;
import com.tequre.wallet.response.report.AgencyAgentsAnalytics;
import com.tequre.wallet.response.report.AgencyAgentsBillEntry;
import com.tequre.wallet.response.report.AgencyAgentsCommissionEntry;
import com.tequre.wallet.response.report.AgencyDivisionReportEntry;
import com.tequre.wallet.response.report.AgencyReportEntry;
import com.tequre.wallet.response.report.AgencyTransactionReportEntry;
import com.tequre.wallet.response.report.AgencyTypeCollectionReportEntry;
import com.tequre.wallet.response.report.AgencyTypeDivisionReportEntry;
import com.tequre.wallet.response.report.DiscomAgencyReportEntry;
import com.tequre.wallet.response.report.DiscomAgentReport;
import com.tequre.wallet.response.report.DiscomAgentReportEntry;
import com.tequre.wallet.response.report.DiscomDivisionReport;
import com.tequre.wallet.response.report.DiscomDivisionReportEntry;
import com.tequre.wallet.response.report.ReconTransactionEntry;
import com.tequre.wallet.response.report.WalletDistributionReport;
import com.tequre.wallet.response.report.WalletDistributionReportEntry;
import com.tequre.wallet.response.report.WalletReport;
import com.tequre.wallet.response.report.WalletReportEntry;
import com.tequre.wallet.service.DivisionService;
import com.tequre.wallet.service.RuralUrbanMappingService;
import com.tequre.wallet.utils.CommonUtils;
import com.tequre.wallet.utils.Constants;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static com.tequre.wallet.utils.Constants.UNKNOWN;

@RestController
@RequestMapping("/v1/report")
public class ReportController {

    private final Logger logger = LoggerFactory.getLogger(ReportController.class);

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoClient mongoClient;

    @Autowired
    private Gson gson;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private RuralUrbanMappingService ruralUrbanMappingService;

    @Autowired
    private DivisionService divisionService;

    // 23-01-2020 07:22:12 AM "dd-MM-yyyy hh:mm:ss aa" // "yyyy-MM-dd HH:mm:ss z"
    private static SimpleDateFormat JDF = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss aa");

    @RequestMapping(value = "/recon", method = RequestMethod.GET, produces = "text/csv")
    public void getTransactions(HttpServletResponse response,
                                @RequestParam(value = "startTime") Long startTime,
                                @RequestParam(value = "endTime") Long endTime) throws IOException {
        JDF.setTimeZone(TimeZone.getTimeZone("IST"));
        List<Transaction> transactions = getPaymentTransactions(startTime, endTime);
        List<ReconTransactionEntry> reportTransactions = transactions.parallelStream().map(transaction -> {
            ReconTransactionEntry reconTransactionEntry = new ReconTransactionEntry();
            reconTransactionEntry.setTransactionId(transaction.getExternalTransactionId());
            reconTransactionEntry.setAgentVan(transaction.getVanId());
            reconTransactionEntry.setAgentName(getAgentOrAgencyName(transaction.getVanId()));
            reconTransactionEntry.setSource(transaction.getTransactionType());
            reconTransactionEntry.setConsumerId(transaction.getConsumerId());
            reconTransactionEntry.setBillNumber(transaction.getBillId());
            reconTransactionEntry.setAmount(transaction.getAmount());
            reconTransactionEntry.setDiscom(transaction.getDiscom());
            reconTransactionEntry.setDivison(transaction.getDivision());
            reconTransactionEntry.setTransactionDate(getTime(transaction.getTransactionTime()));
            return reconTransactionEntry;
        }).sorted(Comparator.comparing(ReconTransactionEntry::getTransactionDate))
                .collect(Collectors.toList());

        String filename = "transactions_" + new Date().toString().replaceAll(" ", "_") + ".csv";
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + filename + "\"");
        try (
                CSVPrinter csvPrinter = new CSVPrinter(response.getWriter(), CSVFormat.DEFAULT
                        .withHeader("TransactionId", "AgentVAN", "AgentName", "Source", "ConsumerId",
                                "BillNumber", "Amount", "Discom", "Division", "TransactionDate"));
        ) {
            for (ReconTransactionEntry transaction : reportTransactions) {
                csvPrinter.printRecord(Arrays.asList(transaction.getTransactionId(), transaction.getAgentVan(), transaction.getAgentName(),
                        transaction.getSource(), transaction.getConsumerId(), transaction.getBillNumber(), transaction.getAmount(),
                        transaction.getDiscom(), transaction.getDivison(), transaction.getTransactionDate()));
            }
            csvPrinter.flush();
        } catch (Exception e) {
            logger.error("Unable to create report for startTime " + startTime + " endTime " + endTime, e);
            throw new IOException("Unable to create report for startTime " + startTime + " endTime " + endTime);
        }
    }

    @RequestMapping(value = "/bill/discom/agent", method = RequestMethod.GET)
    public ResponseEntity<?> getBillCollectionByDiscomByAgent(@RequestParam(value = "discom", required = false) String discom,
                                                              @RequestParam(value = "van", required = false) String van,
                                                              @RequestParam(value = "transactionType", required = false) PaymentType transactionType,
                                                              @RequestParam(value = "agentType", required = false) AgentType agentType,
                                                              @RequestParam(value = "startTime", required = false) Long startTime,
                                                              @RequestParam(value = "endTime", required = false) Long endTime) {

        List<String> vanIds = null;
        if (van != null) {
            Agent queryAgent = new Agent();
            queryAgent.setVan(van);
            Optional<Agent> optAgent = agentRepository.findOne(Example.of(queryAgent));
            if (!optAgent.isPresent()) {
                ServiceMessage serviceMessage = new ServiceMessage();
                serviceMessage.setMessage("No agent/agency exist with van " + van);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
            }
            vanIds = new ArrayList<>();
            vanIds.add(van);
            if (optAgent.get().getAgentType() == AgentType.AGENCY) {
                vanIds.addAll(getAgentsForAgency(optAgent.get().getId()));
            }
        }
        List<Transaction> transactions = getPaymentTransactionsByVanIds(vanIds, discom, null, transactionType, null, null, false, startTime, endTime);
        Map<String, List<Transaction>> discomMap = transactions.stream()
                .filter(txn -> txn.getDiscom() != null)
                .collect(Collectors.groupingBy(Transaction::getDiscom));
        Map<String, Map<String, List<Transaction>>> discomVanMap = discomMap.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey(),
                        entry -> entry.getValue().stream()
                                .filter(txn -> txn.getVanId() != null)
                                .collect(Collectors.groupingBy(Transaction::getVanId))));
        DiscomAgentReport discomAgentReport = new DiscomAgentReport();
        List<DiscomAgentReportEntry> entries = new ArrayList<>();
        Set<DiscomAgencyReportEntry> agencies = new HashSet<>();
        discomVanMap.entrySet().forEach(entry -> {
            entry.getValue().forEach((key, value) -> {
                DiscomAgentReportEntry discomAgentReportEntry = new DiscomAgentReportEntry();
                discomAgentReportEntry.setDiscom(entry.getKey());
                discomAgentReportEntry.setVan(key);
                Agent agent = getAgentByVan(key);
                if (agent == null) {
                    return;
                }
                discomAgentReportEntry.setDistrict(agent.getDistrict());
                if (agent.getAgencyId() != null) {
                    discomAgentReportEntry.setAgencyId(agent.getAgencyId());
                    DiscomAgencyReportEntry discomAgencyReportEntry = new DiscomAgencyReportEntry();
                    discomAgencyReportEntry.setAgencyId(agent.getAgencyId());
                    discomAgencyReportEntry.setDiscom(entry.getKey());
                    agencies.add(discomAgencyReportEntry);
                }
                discomAgentReportEntry.setAgentType(agent.getAgentType());
                discomAgentReportEntry.setName(getAgentOrAgencyName(agent));
                discomAgentReportEntry.setCurrentBalanceAmount(getBalance(agent.getVan()));
                if (agent.getAgentType() == AgentType.AGENCY) {
                    discomAgentReportEntry.setTotalAgents(findAgentsForAgency(agent.getId()));
                }
                Double sum = 0d;
                for (Transaction txn : value) {
                    sum += txn.getAmount();
                }
                discomAgentReportEntry.setTotalBillCollection(sum);
                Map<String, List<Transaction>> billTxns = value.stream()
                        .filter(txn -> txn.getBillId() != null)
                        .collect(Collectors.groupingBy(Transaction::getBillId));
                discomAgentReportEntry.setTotalBillCount(billTxns.size());
                entries.add(discomAgentReportEntry);
            });
        });

        Map<String, List<DiscomAgentReportEntry>> groupByAgencyId = entries.parallelStream().filter(entry -> entry.getAgencyId() != null)
                .collect(Collectors.groupingBy(DiscomAgentReportEntry::getAgencyId));
        agencies.forEach(agencyEntry -> {
            Optional<Agent> agencyOpt = agentRepository.findById(agencyEntry.getAgencyId());
            if (agencyOpt.isPresent()) {
                Agent agency = agencyOpt.get();
                Optional<DiscomAgentReportEntry> discomAgentEntryOpt = entries.parallelStream()
                        .filter(entry -> entry.getVan().equals(agency.getVan()) && entry.getDiscom().equals(agencyEntry.getDiscom()))
                        .findFirst();
                DiscomAgentReportEntry discomAgentReportEntry = null;
                if (discomAgentEntryOpt.isPresent()) {
                    discomAgentReportEntry = discomAgentEntryOpt.get();
                    entries.remove(discomAgentReportEntry);
                } else {
                    discomAgentReportEntry = new DiscomAgentReportEntry();
                    discomAgentReportEntry.setDiscom(agencyEntry.getDiscom());
                    discomAgentReportEntry.setDistrict(agency.getDistrict());
                    discomAgentReportEntry.setVan(agency.getVan());
                    discomAgentReportEntry.setAgentType(agency.getAgentType());
                    discomAgentReportEntry.setName(getAgentOrAgencyName(agency));
                    discomAgentReportEntry.setCurrentBalanceAmount(getBalance(agency.getVan()));
                    discomAgentReportEntry.setTotalAgents(findAgentsForAgency(agency.getId()));
                }
                List<DiscomAgentReportEntry> agentDiscomEntries = groupByAgencyId.get(agencyEntry.getAgencyId());
                Double sum = discomAgentReportEntry.getTotalBillCollection() != null ? discomAgentReportEntry.getTotalBillCollection() : 0d;
                sum += agentDiscomEntries.stream().filter(ade -> ade.getDiscom().equals(agencyEntry.getDiscom())).mapToDouble(DiscomAgentReportEntry::getTotalBillCollection).sum();
                discomAgentReportEntry.setTotalBillCollection(sum);
                int billCounts = discomAgentReportEntry.getTotalBillCount();
                billCounts += agentDiscomEntries.stream().filter(ade -> ade.getDiscom().equals(agencyEntry.getDiscom())).mapToDouble(DiscomAgentReportEntry::getTotalBillCount).sum();
                discomAgentReportEntry.setTotalBillCount(billCounts);
                discomAgentReportEntry.setTotalActiveAgents(agentDiscomEntries.size());
                entries.add(discomAgentReportEntry);
            }
        });
        List<DiscomAgentReportEntry> sortedEntries = null;
        if (agentType != null) {
            Map<AgentType, List<DiscomAgentReportEntry>> groupByAgentType = entries.parallelStream().collect(Collectors.groupingBy(DiscomAgentReportEntry::getAgentType));
            sortedEntries = groupByAgentType.getOrDefault(agentType, Collections.emptyList());
        } else {
            sortedEntries = entries;
        }
        Set<String> vans = sortedEntries.parallelStream().map(entry -> entry.getVan()).collect(Collectors.toSet());
        List<Transaction> creditTxns = getWalletRecharge(vans, startTime, endTime);
        Map<String, List<Transaction>> groupCreditTxnByVanId = creditTxns.parallelStream().collect(Collectors.groupingBy(Transaction::getVanId));
        sortedEntries.forEach(entry -> {
            String entryVan = entry.getVan();
            List<Transaction> credits = groupCreditTxnByVanId.getOrDefault(entryVan, Collections.emptyList());
            Double sum = 0d;
            if (!credits.isEmpty()) {
                sum += credits.stream().mapToDouble(Transaction::getAmount).sum();
            }
            entry.setTotalWalletRecharge(sum);
        });
        sortedEntries.sort(Comparator.comparing(DiscomAgentReportEntry::getDiscom));
        discomAgentReport.setItems(sortedEntries);
        return ResponseEntity.status(HttpStatus.OK).body(discomAgentReport);
    }

    @RequestMapping(value = "/bill/agency", method = RequestMethod.GET)
    public ResponseEntity<?> getBillCollectionForAgency(@RequestParam(value = "discom", required = false) String discom,
                                                        @RequestParam(value = "division", required = false) String division,
                                                        @RequestParam(value = "van", required = false) String van,
                                                        @RequestParam(value = "transactionType", required = false) PaymentType transactionType,
                                                        @RequestParam(value = "consumerId", required = false) String consumerId,
                                                        @RequestParam(value = "startTime", required = false) Long startTime,
                                                        @RequestParam(value = "endTime", required = false) Long endTime) {

        Set<Agent> totalAgencies = new HashSet<>();
        Agent queryAgent = new Agent();
        queryAgent.setAgentType(AgentType.AGENCY);
        if (van != null) {
            queryAgent.setVan(van);
            Optional<Agent> optAgent = agentRepository.findOne(Example.of(queryAgent));
            if (!optAgent.isPresent()) {
                ServiceMessage serviceMessage = new ServiceMessage();
                serviceMessage.setMessage("No agency exist with van " + van);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
            }
            Agent fetchedAgency = optAgent.get();
            if (fetchedAgency.getAgentType() != AgentType.AGENCY) {
                ServiceMessage serviceMessage = new ServiceMessage();
                serviceMessage.setMessage("No agency exist with van " + van);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
            }
            totalAgencies.add(fetchedAgency);
        } else {
            List<Agent> agencies = agentRepository.findAll(Example.of(queryAgent));
            if (agencies != null && !agencies.isEmpty()) {
                totalAgencies.addAll(agencies);
            }
        }
        Set<String> vanIds = new HashSet<>();
        Map<String, Agent> agencies = new HashMap<>();
        totalAgencies.forEach(agency -> {
            agencies.put(agency.getId(), agency);
            vanIds.add(agency.getVan()); // self agency
            vanIds.addAll(getAgentsForAgency(agency.getId())); // agency-agents
        });
        List<Transaction> transactions = getPaymentTransactionsByVanIds(new ArrayList<>(vanIds), discom, division, transactionType, null, consumerId, false, startTime, endTime);
        List<AgencyTransactionReportEntry> reportTransactions = transactions.parallelStream().map(transaction -> {
            AgencyTransactionReportEntry agencyTransactionReportEntry = new AgencyTransactionReportEntry();
            agencyTransactionReportEntry.setTransactionId(transaction.getExternalTransactionId());
            Agent agent = getAgentByVan(transaction.getVanId());
            if (agent == null) {
                return null;
            }
            agencyTransactionReportEntry.setAgentId(agent.getId());
            agencyTransactionReportEntry.setAgentType(agent.getAgentType());
            if (agent.getAgentType() == AgentType.AGENCY) {
                agencyTransactionReportEntry.setAgencyName(agent.getAgencyName());
            } else {
                Agent agency = agencies.get(agent.getAgencyId());
                agencyTransactionReportEntry.setAgencyName(agency.getAgencyName());
            }
            agencyTransactionReportEntry.setSource(transaction.getTransactionType());
            agencyTransactionReportEntry.setConsumerId(transaction.getConsumerId());
            agencyTransactionReportEntry.setBillNumber(transaction.getBillId());
            agencyTransactionReportEntry.setAmount(transaction.getAmount());
            agencyTransactionReportEntry.setDiscom(transaction.getDiscom());
            agencyTransactionReportEntry.setDivision(transaction.getDivision());
            agencyTransactionReportEntry.setTransactionTime(getTime(transaction.getTransactionTime()));
            return agencyTransactionReportEntry;
        }).filter(Objects::nonNull).sorted(Comparator.comparing(AgencyTransactionReportEntry::getTransactionTime))
                .collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.OK).body(reportTransactions);
    }

    @RequestMapping(value = "/bill/agencyType", method = RequestMethod.GET)
    public ResponseEntity<?> getBillCollectionForAgencyType(@RequestParam(value = "agencyType", required = false) String agencyType,
                                                            @RequestParam(value = "agencyVan", required = false) String agencyVan,
                                                            @RequestParam(value = "transactionType", required = false) PaymentType transactionType,
                                                            @RequestParam(value = "district", required = false) String district,
                                                            @RequestParam(value = "division", required = false) String division,
                                                            @RequestParam(value = "startTime", required = false) Long startTime,
                                                            @RequestParam(value = "endTime", required = false) Long endTime) {
        Query agencyQuery = new Query();
        if (agencyType != null) {
            agencyQuery.addCriteria(Criteria.where("agencyType").regex("^" + agencyType + "$", "i"));
        }
        if (agencyVan != null) {
            agencyQuery.addCriteria(Criteria.where("van").is(agencyVan));
        }
        agencyQuery.addCriteria(Criteria.where("agentType").is(AgentType.AGENCY.name()));
        if (district != null) {
            agencyQuery.addCriteria(Criteria.where("district").regex("^" + district + "$", "i"));
        }
        List<Agent> totalAgencies = mongoTemplate.find(agencyQuery, Agent.class);
        logger.info("Total agencies {}", totalAgencies.size());

        Map<String, List<Agent>> agencyTypeMap = totalAgencies.stream()
                .collect(Collectors.groupingBy(agent -> agent.getAgencyType() == null ?
                        Constants.OTHERS : agent.getAgencyType()));

        List<AgencyTypeCollectionReportEntry> reportTransactions = new ArrayList<>();

        Long previous = (endTime != null ? endTime : Instant.now().toEpochMilli()) - (24 * 60 * 60 * 1000);
        agencyTypeMap.entrySet().forEach(agencyTypeEntry -> {
            String type = agencyTypeEntry.getKey();
            List<Agent> typeAgencies = agencyTypeEntry.getValue();
            typeAgencies.forEach(agency -> {
                Set<String> vanIds = new HashSet<>();
                Set<String> vanIdsPrevious = new HashSet<>();

                vanIds.add(agency.getVan()); // self agency
                vanIdsPrevious.add(agency.getVan()); // self agency

                List<Agent> agents = getAgentsForAgency(agency.getId(), startTime, endTime); // Current
                vanIds.addAll(agents.stream().map(Agent::getVan).collect(Collectors.toSet())); // agency-agents

                agents = getAgentsForAgency(agency.getId(), startTime, previous); // Previous
                vanIdsPrevious.addAll(agents.stream().map(Agent::getVan).collect(Collectors.toSet())); // agency-agents

                List<Transaction> transactions = getPaymentTransactionsByVanIds(new ArrayList<>(vanIds), null, division, transactionType, null, null, false, startTime, endTime);
                List<Transaction> transactionsPrevious = getPaymentTransactionsByVanIds(new ArrayList<>(vanIdsPrevious), null, division, transactionType, null, null, false, startTime, previous);

                Set<String> ruralDivisionCode = transactions.stream()
                        .filter(txn -> PaymentType.NON_RAPDRP.toString().equalsIgnoreCase(txn.getTransactionType()))
                        .map(txn -> txn.getDivisionCode() != null ? txn.getDivisionCode() : UNKNOWN).collect(Collectors.toSet());
                ruralDivisionCode.addAll(transactionsPrevious.stream()
                        .filter(txn -> PaymentType.NON_RAPDRP.toString().equalsIgnoreCase(txn.getTransactionType()))
                        .map(txn -> txn.getDivisionCode() != null ? txn.getDivisionCode() : UNKNOWN).collect(Collectors.toSet()));

                Set<String> urbanDivisionCode = transactions.stream()
                        .filter(txn -> PaymentType.RAPDRP.toString().equalsIgnoreCase(txn.getTransactionType()))
                        .map(txn -> txn.getDivisionCode() != null ? txn.getDivisionCode() : UNKNOWN).collect(Collectors.toSet());
                urbanDivisionCode.addAll(transactionsPrevious.stream()
                        .filter(txn -> PaymentType.RAPDRP.toString().equalsIgnoreCase(txn.getTransactionType()))
                        .map(txn -> txn.getDivisionCode() != null ? txn.getDivisionCode() : UNKNOWN).collect(Collectors.toSet()));

                Map<String, RuralUrbanMappingResponse> ruralMapping = divisionCodeMapping(PaymentType.NON_RAPDRP, ruralDivisionCode);
                Map<String, RuralUrbanMappingResponse> urbanMapping = divisionCodeMapping(PaymentType.RAPDRP, urbanDivisionCode);
                Map<String, List<Transaction>> discomTxnMap = transactions.stream()
                        .collect(Collectors.groupingBy(txn -> txn.getDiscom() == null ? UNKNOWN : txn.getDiscom()));
                Map<String, List<Transaction>> discomTxnMapPrev = transactionsPrevious.stream()
                        .collect(Collectors.groupingBy(txn -> txn.getDiscom() == null ? UNKNOWN : txn.getDiscom()));
                Set<String> discoms = new HashSet<>();
                discoms.addAll(discomTxnMap.keySet());
                discoms.addAll(discomTxnMapPrev.keySet());
                for (String discomEntry : discoms) {
                    List<Transaction> discomTransactions = discomTxnMap.get(discomEntry);
                    List<Transaction> discomTransactionsPrev = discomTxnMapPrev.get(discomEntry);
                    Set<String> txnIds = discomTransactions != null ? discomTransactions.stream().map(Transaction::getId)
                            .collect(Collectors.toSet()) : Collections.emptySet();
                    Set<String> txnIdsPrev = discomTransactionsPrev != null ? discomTransactionsPrev.stream().map(Transaction::getId)
                            .collect(Collectors.toSet()) : Collections.emptySet();
                    Map<RuralUrbanMappingResponse, Set<Transaction>> mappings = new HashMap<>();
                    getUrbanMappings(discomTransactions, urbanMapping, mappings);
                    getRuralMappings(discomTransactions, ruralMapping, mappings);
                    getUrbanMappings(discomTransactionsPrev, urbanMapping, mappings);
                    getRuralMappings(discomTransactionsPrev, ruralMapping, mappings);
                    mappings.forEach((response, txns) -> {
                        AgencyTypeCollectionReportEntry reportEntry = new AgencyTypeCollectionReportEntry();
                        reportEntry.setAgencyType(type);
                        reportEntry.setAgencyName(agency.getAgencyName());
                        reportEntry.setDistrict(agency.getDistrict());
                        reportEntry.setDiscom(discomEntry);
                        if (response != null) {
                            reportEntry.setZone(response.getZoneName());
                            reportEntry.setDistrict(response.getDistrictName());
                            reportEntry.setCircle(response.getCircleName());
                            reportEntry.setDivision(response.getDivisionName());
                        }
                        Set<String> billIds = txns.stream().filter(txn -> txnIds.contains(txn.getId())).map(Transaction::getBillId)
                                .filter(Objects::nonNull).collect(Collectors.toSet());
                        Set<String> billIdsPrev = txns.stream().filter(txn -> txnIdsPrev.contains(txn.getId())).map(Transaction::getBillId)
                                .filter(Objects::nonNull).collect(Collectors.toSet());
                        reportEntry.setTotalBillCount(billIds.size());
                        reportEntry.setTotalBillCountPreviousDay(billIdsPrev.size());
                        Set<String> totalActiveAgents = txns.stream().filter(txn -> txnIds.contains(txn.getId())).map(Transaction::getEntityId)
                                .filter(Objects::nonNull).collect(Collectors.toSet());
                        Set<String> totalActiveAgentsPrev = txns.stream().filter(txn -> txnIdsPrev.contains(txn.getId())).map(Transaction::getEntityId)
                                .filter(Objects::nonNull).collect(Collectors.toSet());
                        reportEntry.setTotalActiveAgents(totalActiveAgents.size());
                        reportEntry.setTotalActiveAgentsPreviousDay(totalActiveAgentsPrev.size());
                        reportEntry.setTotalBillCollection(txns.stream().filter(txn -> txnIds.contains(txn.getId()))
                                .mapToDouble(Transaction::getAmount).sum());
                        reportEntry.setTotalBillCollectionPreviousDay(txns.stream().filter(txn -> txnIdsPrev.contains(txn.getId()))
                                .mapToDouble(Transaction::getAmount).sum());
                        reportTransactions.add(reportEntry);
                    });
                }
            });
        });
        reportTransactions.stream().sorted(Comparator.comparing(AgencyTypeCollectionReportEntry::getAgencyType)).collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.OK).body(reportTransactions);
    }

    @RequestMapping(value = "/agency", method = RequestMethod.GET)
    public ResponseEntity<?> getReportForAgency(@RequestParam(value = "discom", required = false) String discom,
                                                @RequestParam(value = "district", required = false) String district,
                                                @RequestParam(value = "agencyName", required = false) String agencyName,
                                                @RequestParam(value = "startTime", required = false) Long startTime,
                                                @RequestParam(value = "endTime", required = false) Long endTime) {
        Query agencyQuery = new Query();
        if (district != null) {
            agencyQuery.addCriteria(Criteria.where("district").regex("^" + district + "$", "i"));
        }
        if (agencyName != null) {
            agencyQuery.addCriteria(Criteria.where("agencyName").regex("^" + agencyName + "$", "i"));
        }
        agencyQuery.addCriteria(Criteria.where("agentType").is(AgentType.AGENCY.name()));
        List<Agent> totalAgencies = mongoTemplate.find(agencyQuery, Agent.class);
        logger.info("Total agencies {}", totalAgencies.size());
        Set<String> vanIds = new HashSet<>();
        Set<String> vanIdsPrevious = new HashSet<>();
        Map<String, AgencyAgents> agencies = new HashMap<>();
        Map<String, String> agentToAgency = new HashMap<>();
        Long previous = (endTime != null ? endTime : Instant.now().toEpochMilli()) - (24 * 60 * 60 * 1000);
        Set<String> agencyVans = new HashSet<>();
        totalAgencies.forEach(agency -> {
            AgencyAgents agencyAgents = new AgencyAgents();
            agencyAgents.setAgency(agency);
            agencyVans.add(agency.getVan());
            vanIds.add(agency.getVan()); // self agency
            vanIdsPrevious.add(agency.getVan());
            List<Agent> agents = getAgentsForAgency(agency.getId(), startTime, endTime);
            agents.forEach(agent -> {
                agentToAgency.put(agent.getId(), agency.getId());
            });
            agencyAgents.setAgentVans(agents.stream().map(subAgent -> subAgent.getVan()).collect(Collectors.toSet()));
            vanIds.addAll(agencyAgents.getAgentVans()); // agency-agents
            agents = getAgentsForAgency(agency.getId(), startTime, previous);
            agencyAgents.setAgentVansPrevious(agents.stream().map(subAgent -> subAgent.getVan()).collect(Collectors.toSet()));
            vanIdsPrevious.addAll(agencyAgents.getAgentVansPrevious()); // agency-agents
            agencies.put(agency.getId(), agencyAgents);
        });
        List<Transaction> transactions = getPaymentTransactionsByVanIds(new ArrayList<>(vanIds), discom, null, null, null, null, false, startTime, endTime);
        List<Transaction> transactionsPrevious = getPaymentTransactionsByVanIds(new ArrayList<>(vanIdsPrevious), discom, null, null, null, null, false, startTime, previous);
        List<Transaction> rechargeTransactions = getWalletRecharge(agencyVans, startTime, endTime);
        List<Transaction> rechargeTransactionsPrevious = getWalletRecharge(agencyVans, startTime, previous);

        Map<String, List<Transaction>> discomTxnMap = transactions.stream()
                .filter(txn -> txn.getDiscom() != null)
                .collect(Collectors.groupingBy(Transaction::getDiscom));
        Map<String, List<Transaction>> discomPrevTxnMap = transactionsPrevious.stream()
                .filter(txn -> txn.getDiscom() != null)
                .collect(Collectors.groupingBy(Transaction::getDiscom));

        Set<String> discoms = new HashSet<>();
        discoms.addAll(discomTxnMap.keySet());
        discoms.addAll(discomPrevTxnMap.keySet());

        Map<String, AgencyReportEntry> reportEntries = new HashMap<>();
        for (String dscm : discoms) {
            List<Transaction> dscmTxns = discomTxnMap.get(dscm);
            Map<String, List<Transaction>> dscmEntityMap = dscmTxns.stream().collect(Collectors.groupingBy(Transaction::getEntityId));
            dscmEntityMap.keySet().forEach(key -> {
                String agencyId = agentToAgency.get(key);
                if (agencyId != null) {
                    AgencyReportEntry agencyReportEntry = reportEntries.get(agencyId);
                    if (agencyReportEntry == null) {
                        AgencyAgents agencyAgents = agencies.get(agencyId);
                        if (agencyAgents != null) {
                            agencyReportEntry = new AgencyReportEntry();
                            agencyReportEntry.setDiscom(dscm);
                            agencyReportEntry.setAgencyName(agencyAgents.getAgency().getAgencyName());
                            agencyReportEntry.setDistrict(agencyAgents.getAgency().getDistrict());
                            agencyReportEntry.setTotalAgents(agencyAgents.getAgentVans() != null ? agencyAgents.getAgentVans().size() : 0);
                            agencyReportEntry.setTotalAgentsPreviousDay(agencyAgents.getAgentVansPrevious() != null ? agencyAgents.getAgentVansPrevious().size() : 0);
                            List<Transaction> rechargeTxns = rechargeTransactions.stream()
                                    .filter(txn -> agencyId.equalsIgnoreCase(txn.getEntityId()))
                                    .collect(Collectors.toList());
                            Double rechargeTxnsSum = 0d;
                            for (Transaction txn : rechargeTxns) {
                                rechargeTxnsSum += txn.getAmount();
                            }
                            agencyReportEntry.setWalletRecharge(rechargeTxnsSum);
                            List<Transaction> rechargeTxnsPrev = rechargeTransactionsPrevious.stream()
                                    .filter(txn -> agencyId.equalsIgnoreCase(txn.getEntityId()))
                                    .collect(Collectors.toList());
                            Double rechargeTxnsSumPrev = 0d;
                            for (Transaction txn : rechargeTxnsPrev) {
                                rechargeTxnsSumPrev += txn.getAmount();
                            }
                            agencyReportEntry.setWalletRechargePreviousDay(rechargeTxnsSumPrev);
                            reportEntries.put(agencyId, agencyReportEntry);
                        } else {
                            return;
                        }
                    }
                    List<Transaction> txns = dscmEntityMap.get(key);
                    Set<String> billIds = txns.stream().filter(txn -> txn.getBillId() != null).map(Transaction::getBillId).collect(Collectors.toSet());
                    Double billCollection = 0d;
                    for (Transaction txn : txns) {
                        billCollection += txn.getAmount();
                    }
                    agencyReportEntry.setBillCount(agencyReportEntry.getBillCount() + billIds.size());
                    agencyReportEntry.setBillCollection(agencyReportEntry.getBillCollection() + billCollection);
                    agencyReportEntry.setActiveAgents(agencyReportEntry.getActiveAgents() + 1);

                    List<Transaction> dscmPrevTxns = discomPrevTxnMap.get(dscm);
                    if (dscmPrevTxns != null && !dscmPrevTxns.isEmpty()) {
                        Map<String, List<Transaction>> dscmPrevEntityMap = dscmPrevTxns.stream().collect(Collectors.groupingBy(Transaction::getEntityId));
                        List<Transaction> prevTxns = dscmPrevEntityMap.get(key);
                        if (prevTxns != null && !prevTxns.isEmpty()) {
                            Set<String> prevBillIds = prevTxns.stream().filter(txn -> txn.getBillId() != null).map(Transaction::getBillId).collect(Collectors.toSet());
                            Double prevBillCollection = 0d;
                            for (Transaction txn : prevTxns) {
                                prevBillCollection += txn.getAmount();
                            }
                            agencyReportEntry.setBillCountPreviousDay(agencyReportEntry.getBillCountPreviousDay() + prevBillIds.size());
                            agencyReportEntry.setBillCollectionPreviousDay(agencyReportEntry.getBillCollectionPreviousDay() + prevBillCollection);
                            agencyReportEntry.setActiveAgentsPreviousDay(agencyReportEntry.getActiveAgentsPreviousDay() + 1);
                        }
                    }
                }
            });
        }
        return ResponseEntity.status(HttpStatus.OK).body(reportEntries.values());
    }

    @RequestMapping(value = "/agency/division", method = RequestMethod.GET)
    public ResponseEntity<?> getDivisionReportForAgency(@RequestParam(value = "discom", required = false) String discom,
                                                        @RequestParam(value = "district", required = false) String district,
                                                        @RequestParam(value = "agencyName", required = false) String agencyName,
                                                        @RequestParam(value = "startTime", required = false) Long startTime,
                                                        @RequestParam(value = "endTime", required = false) Long endTime) {
        Query agencyQuery = new Query();
        if (district != null) {
            agencyQuery.addCriteria(Criteria.where("district").regex("^" + district + "$", "i"));
        }
        if (agencyName != null) {
            agencyQuery.addCriteria(Criteria.where("agencyName").regex("^" + agencyName + "$", "i"));
        }
        agencyQuery.addCriteria(Criteria.where("agentType").is(AgentType.AGENCY.name()));
        List<Agent> totalAgencies = mongoTemplate.find(agencyQuery, Agent.class);
        logger.info("Total agencies {}", totalAgencies.size());
        Set<String> vanIds = new HashSet<>();
        Map<String, AgencyAgents> agencies = new HashMap<>();
        Map<String, String> agentToAgency = new HashMap<>();
        Set<String> agencyVans = new HashSet<>();
        totalAgencies.forEach(agency -> {
            AgencyAgents agencyAgents = new AgencyAgents();
            agentToAgency.put(agency.getId(), agency.getId());
            agencyAgents.setAgency(agency);
            agencyVans.add(agency.getVan());
            vanIds.add(agency.getVan()); // self agency
            List<Agent> agents = getAgentsForAgency(agency.getId(), startTime, endTime);
            agents.forEach(agent -> {
                agentToAgency.put(agent.getId(), agency.getId());
            });
            agencyAgents.setAgentVans(agents.stream().map(subAgent -> subAgent.getVan()).collect(Collectors.toSet()));
            vanIds.addAll(agencyAgents.getAgentVans()); // agency-agents
            agencies.put(agency.getId(), agencyAgents);
        });
        List<Transaction> transactions = getPaymentTransactionsByVanIds(new ArrayList<>(vanIds), discom, null, null, null, null, false, startTime, endTime);

        Set<String> ruralDivisionCode = transactions.stream()
                .filter(txn -> PaymentType.NON_RAPDRP.toString().equalsIgnoreCase(txn.getTransactionType()))
                .map(txn -> txn.getDivisionCode()).collect(Collectors.toSet());
        Set<String> urbanDivisionCode = transactions.stream()
                .filter(txn -> PaymentType.RAPDRP.toString().equalsIgnoreCase(txn.getTransactionType()))
                .map(txn -> txn.getDivisionCode()).collect(Collectors.toSet());

        Map<String, RuralUrbanMappingResponse> ruralMapping = divisionCodeMapping(PaymentType.NON_RAPDRP, ruralDivisionCode);
        Map<String, RuralUrbanMappingResponse> urbanMapping = divisionCodeMapping(PaymentType.RAPDRP, urbanDivisionCode);

        Map<String, List<Transaction>> discomMap = transactions.stream()
                .filter(txn -> txn.getDiscom() != null)
                .collect(Collectors.groupingBy(Transaction::getDiscom));
        List<AgencyDivisionReportEntry> reportEntries = new ArrayList<>();
        discomMap.entrySet().forEach(entry -> {
            String dscm = entry.getKey();
            List<Transaction> discomTxns = entry.getValue();
            Map<String, List<Transaction>> agencyEntries = new HashMap<>();
            discomTxns.forEach(txn -> {
                String entityId = txn.getEntityId();
                String agencyId = agentToAgency.get(entityId);
                List<Transaction> txns = agencyEntries.get(agencyId);
                if (txns == null) {
                    txns = new ArrayList<>();
                }
                txns.add(txn);
                agencyEntries.put(agencyId, txns);
            });
            agencyEntries.entrySet().forEach(agencyEntry -> {
                String agencyId = agencyEntry.getKey();
                AgencyAgents agencyAgents = agencies.get(agencyId);
                if (agencyAgents != null) {
                    Map<RuralUrbanMappingResponse, Set<Transaction>> mappings = new HashMap<>();
                    getUrbanMappings(agencyEntry.getValue(), urbanMapping, mappings);
                    getRuralMappings(agencyEntry.getValue(), ruralMapping, mappings);
                    mappings.entrySet().forEach(map -> {
                        RuralUrbanMappingResponse response = map.getKey();
                        Set<Transaction> txns = map.getValue();
                        AgencyDivisionReportEntry agencyDivisionReportEntry = new AgencyDivisionReportEntry();
                        agencyDivisionReportEntry.setDiscom(dscm);
                        agencyDivisionReportEntry.setAgencyName(agencyAgents.getAgency().getAgencyName());
                        agencyDivisionReportEntry.setAgencyDistrict(agencyAgents.getAgency().getDistrict());
                        if (response != null) {
                            agencyDivisionReportEntry.setZone(response.getZoneName());
                            agencyDivisionReportEntry.setDistrict(response.getDistrictName());
                            agencyDivisionReportEntry.setCircle(response.getCircleName());
                            agencyDivisionReportEntry.setDivision(response.getDivisionName());
                        }
                        Set<String> urbanAgents = new HashSet<>();
                        Set<String> ruralAgents = new HashSet<>();
                        Set<String> urbanBillIds = new HashSet<>();
                        Set<String> ruralBillIds = new HashSet<>();
                        txns.forEach(txn -> {
                            if (PaymentType.NON_RAPDRP.toString().equalsIgnoreCase(txn.getTransactionType())) {
                                if (!agencyId.equalsIgnoreCase(txn.getEntityId())) {
                                    ruralAgents.add(txn.getEntityId());
                                }
                                ruralBillIds.add(txn.getBillId());
                                agencyDivisionReportEntry.setRuralBillCollection(agencyDivisionReportEntry.getRuralBillCollection() + txn.getAmount());
                            } else if (PaymentType.RAPDRP.toString().equalsIgnoreCase(txn.getTransactionType())) {
                                if (!agencyId.equalsIgnoreCase(txn.getEntityId())) {
                                    urbanAgents.add(txn.getEntityId());
                                }
                                urbanBillIds.add(txn.getBillId());
                                agencyDivisionReportEntry.setUrbanBillCollection(agencyDivisionReportEntry.getUrbanBillCollection() + txn.getAmount());
                            }
                        });
                        agencyDivisionReportEntry.setUrbanActiveAgents(urbanAgents.size());
                        agencyDivisionReportEntry.setUrbanBillCount(urbanBillIds.size());
                        agencyDivisionReportEntry.setRuralActiveAgents(ruralAgents.size());
                        agencyDivisionReportEntry.setRuralBillCount(ruralBillIds.size());
                        reportEntries.add(agencyDivisionReportEntry);
                    });
                }
            });
        });
        return ResponseEntity.status(HttpStatus.OK).body(reportEntries);
    }

    @RequestMapping(value = "/agencyType/division", method = RequestMethod.GET)
    public ResponseEntity<?> getDivisionReportForAgencyType(@RequestParam(value = "agencyType", required = false) String agencyType,
                                                            @RequestParam(value = "agencyVan", required = false) String agencyVan,
                                                            @RequestParam(value = "transactionType", required = false) PaymentType transactionType,
                                                            @RequestParam(value = "district", required = false) String district,
                                                            @RequestParam(value = "division", required = false) String division,
                                                            @RequestParam(value = "startTime", required = false) Long startTime,
                                                            @RequestParam(value = "endTime", required = false) Long endTime) {
        Query agencyQuery = new Query();
        if (agencyType != null) {
            agencyQuery.addCriteria(Criteria.where("agencyType").regex("^" + agencyType + "$", "i"));
        }
        if (agencyVan != null) {
            agencyQuery.addCriteria(Criteria.where("van").is(agencyVan));
        }
        agencyQuery.addCriteria(Criteria.where("agentType").is(AgentType.AGENCY.name()));
        if (district != null) {
            agencyQuery.addCriteria(Criteria.where("district").regex("^" + district + "$", "i"));
        }
        List<Agent> totalAgencies = mongoTemplate.find(agencyQuery, Agent.class);
        logger.info("Total agencies {}", totalAgencies.size());

        Map<String, List<Agent>> agencyTypeMap = totalAgencies.stream()
                .collect(Collectors.groupingBy(agent -> agent.getAgencyType() == null ?
                        Constants.OTHERS : agent.getAgencyType()));

        List<AgencyTypeDivisionReportEntry> reportEntries = new ArrayList<>();
        agencyTypeMap.forEach((type, typeAgencies) -> {
            Set<String> vanIds = new HashSet<>();
            typeAgencies.forEach(agency -> {
                vanIds.add(agency.getVan()); // self agency
                List<Agent> agents = getAgentsForAgency(agency.getId(), startTime, endTime); // Current
                vanIds.addAll(agents.stream().map(Agent::getVan).collect(Collectors.toSet())); // agency-agents
            });
            List<Transaction> transactions = getPaymentTransactionsByVanIds(new ArrayList<>(vanIds), null, division, transactionType, null, null, false, startTime, endTime);
            Set<String> ruralDivisionCode = transactions.stream()
                    .filter(txn -> PaymentType.NON_RAPDRP.toString().equalsIgnoreCase(txn.getTransactionType()))
                    .map(txn -> txn.getDivisionCode() != null ? txn.getDivisionCode() : UNKNOWN).collect(Collectors.toSet());
            Set<String> urbanDivisionCode = transactions.stream()
                    .filter(txn -> PaymentType.RAPDRP.toString().equalsIgnoreCase(txn.getTransactionType()))
                    .map(txn -> txn.getDivisionCode() != null ? txn.getDivisionCode() : UNKNOWN).collect(Collectors.toSet());
            logger.info("Urban Txn: {}, Rural Txn: {}", urbanDivisionCode.size(), ruralDivisionCode.size());
            Map<String, RuralUrbanMappingResponse> ruralMapping = divisionCodeMapping(PaymentType.NON_RAPDRP, ruralDivisionCode);
            Map<String, RuralUrbanMappingResponse> urbanMapping = divisionCodeMapping(PaymentType.RAPDRP, urbanDivisionCode);
            Map<Optional<String>, List<Transaction>> discomTxnMap = transactions.stream()
                    .collect(Collectors.groupingBy(txn -> Optional.ofNullable(txn.getDiscom())));
            discomTxnMap.forEach((discomName, discomTransactions) -> {
                Map<RuralUrbanMappingResponse, Set<Transaction>> mappings = new HashMap<>();
                getUrbanMappings(discomTransactions, urbanMapping, mappings);
                getRuralMappings(discomTransactions, ruralMapping, mappings);
                Map<String, List<RuralUrbanMappingResponse>> divisionMap = mappings.keySet().stream()
                        .filter(entry -> entry != null && entry.getDivisionCode() != null && entry.getDivisionName() != null)
                        .collect(Collectors.groupingBy(ruralUrbanMappingResponse -> ruralUrbanMappingResponse.getDivisionCode() + ":"
                                + ruralUrbanMappingResponse.getDivisionName()));
                divisionMap.forEach((key, value) -> {
                    String[] arr = key.split(":");
                    AgencyTypeDivisionReportEntry reportEntry = new AgencyTypeDivisionReportEntry();
                    reportEntry.setAgencyType(type);
                    reportEntry.setDiscom(discomName.isPresent() ? discomName.get() : UNKNOWN);
                    reportEntry.setDivisionCode(arr[0]);
                    reportEntry.setDivision(arr[1]);
                    AtomicInteger totalActiveAgents = new AtomicInteger(0);
                    AtomicDouble totalBillCollection = new AtomicDouble(0);
                    AtomicInteger totalBillCount = new AtomicInteger(0);
                    value.forEach(v -> {
                        Set<Transaction> divisionTransactions = mappings.get(v);
                        if (divisionTransactions != null) {
                            totalBillCount.getAndAdd(divisionTransactions.stream().map(Transaction::getBillId)
                                    .filter(Objects::nonNull).collect(Collectors.toSet()).size());
                            totalActiveAgents.getAndAdd(divisionTransactions.stream().map(Transaction::getEntityId)
                                    .filter(Objects::nonNull).collect(Collectors.toSet()).size());
                            totalBillCollection.getAndAdd(divisionTransactions.stream().mapToDouble(Transaction::getAmount).sum());
                        }
                    });
                    reportEntry.setTotalActiveAgents(totalActiveAgents.get());
                    reportEntry.setTotalBillCollection(totalBillCollection.get());
                    reportEntry.setTotalBillCount(totalBillCount.get());
                    reportEntries.add(reportEntry);
                });
            });
        });
        reportEntries.stream().sorted(Comparator.comparing(AgencyTypeDivisionReportEntry::getAgencyType));
        return ResponseEntity.status(HttpStatus.OK).body(reportEntries);
    }

    @RequestMapping(value = "/agency/analytics", method = RequestMethod.GET)
    public ResponseEntity<?> getAnalyticsForAgency(@RequestParam(value = "agencyName", required = false) String agencyName,
                                                   @RequestParam(value = "agencyType", required = false) String agencyType,
                                                   @RequestParam(value = "breakupByAgency", required = false, defaultValue = "false") boolean breakupByAgency,
                                                   @RequestParam(value = "startTime", required = false) Long startTime,
                                                   @RequestParam(value = "endTime", required = false) Long endTime) {
        Query agencyQuery = new Query();
        if (agencyName != null) {
            agencyQuery.addCriteria(Criteria.where("agencyName").regex("^" + agencyName + "$", "i"));
        }
        if (agencyType != null) {
            agencyQuery.addCriteria(Criteria.where("agencyType").regex("^" + agencyType + "$", "i"));
        }
        agencyQuery.addCriteria(Criteria.where("agentType").is(AgentType.AGENCY.name()));
        List<Agent> totalAgencies = mongoTemplate.find(agencyQuery, Agent.class);
        logger.info("Total agencies {}", totalAgencies.size());
        Map<String, String> agentByAgency = new HashMap<>();
        Map<String, Agent> vanByAgent = new HashMap<>();
        List<AgencyAgentsBillEntry> agentBillEntries = new ArrayList<>();
        AtomicInteger i = new AtomicInteger(0);
        totalAgencies.forEach(agency -> {
            logger.info("Agency No {}: {}", i.getAndIncrement(), getAgentOrAgencyName(agency.getVan()));
            Set<String> vanIds = new HashSet<>();
            vanIds.add(agency.getVan());
            vanByAgent.put(agency.getVan(), agency); // self-agency
            List<Agent> agents = getAgentsForAgency(agency.getId(), null, null);
            agents.forEach(agent -> {
                vanByAgent.put(agent.getVan(), agent);
                agentByAgency.put(agent.getVan(), agency.getVan());
                vanIds.add(agent.getVan());
            });
            List<Transaction> transactions = getPaymentTransactionsByVanIds(new ArrayList<>(vanIds), null, null,
                    null, null, null, false, startTime, endTime);
            Map<String, List<Transaction>> agentTxnMap = transactions.stream()
                    .filter(txn -> txn.getVanId() != null)
                    .collect(Collectors.groupingBy(Transaction::getVanId));
            agentTxnMap.forEach((key, txns) -> {
                AgencyAgentsBillEntry agencyAgentsBillEntry = new AgencyAgentsBillEntry();
                agencyAgentsBillEntry.setVanId(key);
                agencyAgentsBillEntry.setAgencyVanId(agency.getVan());
                List<String> billIds = txns.stream()
                        .filter(txn -> txn.getBillId() != null)
                        .map(Transaction::getBillId).collect(Collectors.toList());
                agencyAgentsBillEntry.setBillCount(billIds.size());
                Double billCollection = 0d;
                for (Transaction txn : txns) {
                    billCollection += txn.getAmount();
                }
                agencyAgentsBillEntry.setBillCollection(billCollection);
                agentBillEntries.add(agencyAgentsBillEntry);
            });
        });
        List<AgencyAgentsAnalytics> result = new ArrayList<>();
        if (breakupByAgency) {
            Map<String, List<AgencyAgentsBillEntry>> mappingByAgency = agentBillEntries.stream()
                    .filter(txn -> txn.getVanId() != null)
                    .collect(Collectors.groupingBy(AgencyAgentsBillEntry::getAgencyVanId));
            mappingByAgency.forEach((key, value) -> {
                Agent agency = vanByAgent.get(key);
                List<AgencyAgentAnalyticsRecord> topBillCount = getTopBillCount(value, vanByAgent, true);
                List<AgencyAgentAnalyticsRecord> topBillCollection = getTopBillCollection(value, vanByAgent, true);
                AgencyAgentsAnalytics agencyAgentsAnalytics = new AgencyAgentsAnalytics();
                agencyAgentsAnalytics.setAgencyId(agency.getId());
                agencyAgentsAnalytics.setAgencyName(getAgentOrAgencyName(agency));
                agencyAgentsAnalytics.setAgencyType(agency.getAgencyType());
                agencyAgentsAnalytics.setTopAgentsByBillCount(topBillCount);
                agencyAgentsAnalytics.setTopAgentsByBillCollection(topBillCollection);
                result.add(agencyAgentsAnalytics);
            });
        } else {
            List<AgencyAgentAnalyticsRecord> topBillCount = getTopBillCount(agentBillEntries, vanByAgent, false);
            List<AgencyAgentAnalyticsRecord> topBillCollection = getTopBillCollection(agentBillEntries, vanByAgent, false);
            AgencyAgentsAnalytics agencyAgentsAnalytics = new AgencyAgentsAnalytics();
            agencyAgentsAnalytics.setTopAgentsByBillCollection(topBillCollection);
            agencyAgentsAnalytics.setTopAgentsByBillCount(topBillCount);
            result.add(agencyAgentsAnalytics);
        }
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @RequestMapping(value = "/agency/commission", method = RequestMethod.GET)
    public ResponseEntity<?> getCommissionForAgentOrAgency(@RequestParam(value = "agencyType") String agencyType,
                                                           @RequestParam(value = "agencyVan", required = false) String agencyVan,
                                                           @RequestParam(value = "agentVan", required = false) String agentVan,
                                                           @RequestParam(value = "startTime", required = false) Long startTime,
                                                           @RequestParam(value = "endTime", required = false) Long endTime) {

        Map<String, List<Agent>> agencyTypeMap = new HashMap<>();
        if (agentVan == null) {
            Query agencyQuery = new Query();
            if (agencyType != null) {
                agencyQuery.addCriteria(Criteria.where("agencyType").regex("^" + agencyType + "$", "i"));
            }
            if (agencyVan != null) {
                agencyQuery.addCriteria(Criteria.where("van").is(agencyVan));
            }
            agencyQuery.addCriteria(Criteria.where("agentType").is(AgentType.AGENCY.name()));
            List<Agent> totalAgencies = mongoTemplate.find(agencyQuery, Agent.class);
            logger.info("Total agencies {}", totalAgencies.size());

            agencyTypeMap = totalAgencies.stream()
                    .collect(Collectors.groupingBy(agent -> agent.getAgencyType() == null ?
                            Constants.OTHERS : agent.getAgencyType()));
        } else {
            Agent agent = findAgentByVanId(agentVan);
            if (agent == null) {
                logger.info("No Agent found with van {}", agentVan);
                return ResponseEntity.status(HttpStatus.OK).body(Collections.emptyList());
            }
            String agencyId = agent.getAgencyId();
            if (agencyId == null) {
                logger.info("No associated agency found with agent van {}", agentVan);
                return ResponseEntity.status(HttpStatus.OK).body(Collections.emptyList());
            }
            Optional<Agent> agencyOpt = agentRepository.findById(agencyId);
            if (!agencyOpt.isPresent()) {
                logger.info("No agency found with id {}", agencyId);
                return ResponseEntity.status(HttpStatus.OK).body(Collections.emptyList());
            }
            Agent agency = agencyOpt.get();
            List<Agent> agents = new ArrayList<>();
            agents.add(agency);
            agencyTypeMap.put(agency.getAgencyType(), agents);
        }

        List<AgencyAgentsCommissionEntry> commissionEntries = new ArrayList<>();
        agencyTypeMap.forEach((type, typeAgencies) -> {
            Map<String, Agent> vanByAgent = new HashMap<>();
            typeAgencies.forEach(agency -> {
                vanByAgent.put(agency.getVan(), agency);
                if (agentVan == null) {
                    List<Agent> agents = getAgentsForAgency(agency.getId(), null, null);
                    agents.forEach(agent -> {
                        vanByAgent.put(agent.getVan(), agent);
                    });
                }
            });
            Set<String> vanIds = vanByAgent.keySet();
            if (agentVan != null) {
                Agent agent = findAgentByVanId(agentVan);
                vanByAgent.put(agent.getVan(), agent);
                vanIds = new HashSet<>();
                vanIds.add(agentVan);
            }
            List<Transaction> commissionTransactions = getCommissionTransactions(new ArrayList<>(vanIds), startTime, endTime);
            logger.info("commissionTransactions: "+commissionTransactions);
            Set<String> externalIds = commissionTransactions.stream().map(Transaction::getExternalTransactionId).collect(Collectors.toSet());
            logger.info("externalIds: "+externalIds);
            List<Transaction> paymentTransactions = getPaymentTransactionsByExternalIds(new ArrayList<>(externalIds));
            logger.info("externalIds: "+externalIds);
            Map<String, Transaction> txnByExternalId = paymentTransactions.stream().collect(Collectors.toMap(Transaction::getExternalTransactionId, txn -> txn));
            commissionTransactions.forEach(commissionEntry -> {
                Transaction paymentTxn = txnByExternalId.get(commissionEntry.getExternalTransactionId());
                if (paymentTxn == null) {
                    logger.warn("Commission found but no payment transaction found for external transaction id {}", commissionEntry.getExternalTransactionId());
                    return;
                }
                Agent agency = vanByAgent.get(commissionEntry.getAgencyVan());
                if (agency == null) {
                    logger.warn("Commission found but no agency found for van {}", commissionEntry.getAgencyVan());
                    return;
                }
                AgencyAgentsCommissionEntry agencyAgentsCommissionEntry = new AgencyAgentsCommissionEntry();
                agencyAgentsCommissionEntry.setTransactionId(commissionEntry.getExternalTransactionId());
                agencyAgentsCommissionEntry.setAgencyType(type);
                if ("MR".equalsIgnoreCase(agencyType) || "PACS".equalsIgnoreCase(agencyType)) {
                	List<Transaction> debitTransaction = getTransactions(commissionEntry.getExternalTransactionId());
                	for(Transaction t:debitTransaction) {
                		logger.info("MR/PACS: "+ t);
                		agencyAgentsCommissionEntry.setAgentAgencyVan(t.getVanId());
                		agencyAgentsCommissionEntry.setAgentAgencyName(getAgentOrAgencyName(t.getVanId()));
                	}
                }  else {
                	agencyAgentsCommissionEntry.setAgentAgencyVan(commissionEntry.getVanId());
                    agencyAgentsCommissionEntry.setAgentAgencyName(getAgentOrAgencyName(commissionEntry.getVanId()));
                }
                // agencyAgentsCommissionEntry.setAgentAgencyVan(commissionEntry.getVanId());
                //  agencyAgentsCommissionEntry.setAgentAgencyName(getAgentOrAgencyName(commissionEntry.getVanId()));
                agencyAgentsCommissionEntry.setSource(paymentTxn.getTransactionType());
                agencyAgentsCommissionEntry.setBillId(commissionEntry.getBillId());
                agencyAgentsCommissionEntry.setConsumerId(commissionEntry.getConsumerId());
                agencyAgentsCommissionEntry.setAmount(paymentTxn.getAmount());
                agencyAgentsCommissionEntry.setType(commissionEntry.getActivity());
                agencyAgentsCommissionEntry.setNetCommission(commissionEntry.getAmount());
                agencyAgentsCommissionEntry.setDiscom(commissionEntry.getDiscom());
                agencyAgentsCommissionEntry.setDivision(commissionEntry.getDivision());
                agencyAgentsCommissionEntry.setBillTime(paymentTxn.getTransactionTime());
                agencyAgentsCommissionEntry.setCommissionTime(commissionEntry.getTransactionTime());
                Double rate = agency.getCommissionRate();
                Double gstRate = agency.getGstRate() == null ? 0d : agency.getGstRate();
                Double tdsRate = agency.getTdsRate() == null ? 0d : agency.getTdsRate();
                Double gstTdsRate = agency.getGstTdsRate() == null ? 0d : agency.getGstTdsRate();
                final DecimalFormat df = new DecimalFormat("0.00");
                df.setRoundingMode(RoundingMode.DOWN);
                if (rate != null) {
                    double commission = paymentTxn.getAmount() * (rate / 100);
                    BigDecimal commissionBD = new BigDecimal(commission).setScale(2, RoundingMode.HALF_UP);
                    // BigDecimal commissionBD = new BigDecimal(df.format(commission));
                    agencyAgentsCommissionEntry.setCommission(commissionBD.doubleValue());
                    double gst = commissionBD.doubleValue() * (gstRate / 100);
                    BigDecimal gstBD = new BigDecimal(gst).setScale(2, RoundingMode.HALF_UP);
                    // BigDecimal gstBD = new BigDecimal(df.format(gst));
                    agencyAgentsCommissionEntry.setGst(gstBD.doubleValue());
                    double tds = commissionBD.doubleValue() * (tdsRate / 100);
                    BigDecimal tdsBD = new BigDecimal(tds).setScale(2, RoundingMode.HALF_UP);
                    // BigDecimal tdsBD = new BigDecimal(df.format(tds));
                    agencyAgentsCommissionEntry.setTds(tdsBD.doubleValue());
                    // double gstTds = tdsBD.doubleValue() * (gstTdsRate / 100);
                    double gstTds = commissionBD.doubleValue() * (gstTdsRate / 100);
                    BigDecimal gstTdsBD = new BigDecimal(gstTds).setScale(2, RoundingMode.HALF_UP);
                    // BigDecimal gstTdsBD = new BigDecimal(df.format(gstTds));
                    agencyAgentsCommissionEntry.setGstOnTds(gstTdsBD.doubleValue());
                }
                commissionEntries.add(agencyAgentsCommissionEntry);
            });
        });
        commissionEntries.sort(Comparator.comparing(AgencyAgentsCommissionEntry::getCommissionTime).reversed());
        commissionEntries.stream().sorted(Comparator.comparing(AgencyAgentsCommissionEntry::getCommissionTime));
        return ResponseEntity.status(HttpStatus.OK).body(commissionEntries);
    }
    
    private List<Transaction> getTransactions(String transactionId) {
        List<Criteria> queryConditions = new ArrayList<>();
        queryConditions.add(Criteria.where("activity").is(TransactionType.DEBIT.name()));
        queryConditions.add(Criteria.where("vanId").ne(Constants.UPPCL_VAN));
        queryConditions.add(Criteria.where("externalTransactionId").is(transactionId));
        Query query = new Query();
        Criteria criteria = new Criteria();
        criteria.andOperator(queryConditions.stream().toArray(Criteria[]::new));
        query.addCriteria(criteria);
        return mongoTemplate.find(query, Transaction.class);
    }

    private List<Transaction> getCommissionTransactions(List<String> vanIds, Long startTime, Long endTime) {
        List<Criteria> queryConditions = new ArrayList<>();
        if (vanIds != null && !vanIds.isEmpty()) {
            queryConditions.add(Criteria.where("vanId").in(vanIds));
        }
        if (startTime != null || endTime != null) {
            Criteria range = Criteria.where("transactionTime");
            if (startTime != null) {
                range.gte(Instant.ofEpochMilli(startTime));
            }
            if (endTime != null) {
                range.lte(Instant.ofEpochMilli(endTime));
            }
            queryConditions.add(range);
        }
        queryConditions.add(Criteria.where("activity").is(TransactionType.CREDIT.name()));
        queryConditions.add(Criteria.where("vanId").ne(Constants.UPPCL_VAN));
        queryConditions.add(Criteria.where("transactionType").is(PaymentType.COMMISSION.toString()));
        Query query = new Query();
        Criteria criteria = new Criteria();
        criteria.andOperator(queryConditions.stream().toArray(Criteria[]::new));
        query.addCriteria(criteria);
        return mongoTemplate.find(query, Transaction.class);
    }

    private List<Transaction> getPaymentTransactionsByExternalIds(List<String> externalIds) {
        List<Criteria> queryConditions = new ArrayList<>();
        if (externalIds != null && !externalIds.isEmpty()) {
            queryConditions.add(Criteria.where("externalTransactionId").in(externalIds));
        }
        queryConditions.add(Criteria.where("activity").is(TransactionType.DEBIT.name()));
        queryConditions.add(Criteria.where("vanId").ne(Constants.UPPCL_VAN));
        queryConditions.add(Criteria.where("transactionType").in(Arrays.asList(PaymentType.NON_RAPDRP.toString(),
                PaymentType.RAPDRP.toString())));
        Query query = new Query();
        Criteria criteria = new Criteria();
        criteria.andOperator(queryConditions.stream().toArray(Criteria[]::new));
        query.addCriteria(criteria);
        return mongoTemplate.find(query, Transaction.class);
    }

    private List<AgencyAgentAnalyticsRecord> getTopBillCount(List<AgencyAgentsBillEntry> agentBillEntries,
                                                             Map<String, Agent> vanByAgent, boolean breakupByAgency) {
        return agentBillEntries.stream()
                .sorted(Comparator.comparing(AgencyAgentsBillEntry::getBillCount).reversed()).limit(5)
                .map(entry -> {
                    AgencyAgentAnalyticsRecord record = new AgencyAgentAnalyticsRecord();
                    record.setAgentVan(entry.getVanId());
                    Agent agent = vanByAgent.get(entry.getVanId());
                    if (!breakupByAgency) {
                        Agent agency = vanByAgent.get(entry.getAgencyVanId());
                        record.setAgencyId(agency.getId());
                        record.setAgencyVan(agency.getVan());
                        record.setAgencyName(getAgentOrAgencyName(agency));
                    }
                    record.setAgentId(agent.getId());
                    record.setAgentName(getAgentOrAgencyName(agent));
                    record.setBillCount(entry.getBillCount());
                    record.setBillCollection(entry.getBillCollection());
                    return record;
                }).collect(Collectors.toList());
    }

    private List<AgencyAgentAnalyticsRecord> getTopBillCollection(List<AgencyAgentsBillEntry> agentBillEntries,
                                                                  Map<String, Agent> vanByAgent, boolean breakupByAgency) {
        return agentBillEntries.stream()
                .sorted(Comparator.comparing(AgencyAgentsBillEntry::getBillCollection).reversed()).limit(5)
                .map(entry -> {
                    AgencyAgentAnalyticsRecord record = new AgencyAgentAnalyticsRecord();
                    record.setAgentVan(entry.getVanId());
                    Agent agent = vanByAgent.get(entry.getVanId());
                    if (!breakupByAgency) {
                        Agent agency = vanByAgent.get(entry.getAgencyVanId());
                        record.setAgencyId(agency.getId());
                        record.setAgencyVan(agency.getVan());
                        record.setAgencyName(getAgentOrAgencyName(agency));
                    }
                    record.setAgentId(agent.getId());
                    record.setAgentName(getAgentOrAgencyName(agent));
                    record.setBillCount(entry.getBillCount());
                    record.setBillCollection(entry.getBillCollection());
                    return record;
                }).collect(Collectors.toList());
    }

    private Map<String, RuralUrbanMappingResponse> divisionCodeMapping(PaymentType paymentType, Set<String> divisionCodes) {
        Map<String, RuralUrbanMappingResponse> divisionCodeMapping = new HashMap<>();
        if (divisionCodes != null) {
            divisionCodes.forEach(code -> {
                RuralUrbanMappingResponse ruralUrbanMappingResponse = null;
                if (Objects.isNull(code) || UNKNOWN.equals(code)) {
                    ruralUrbanMappingResponse = new RuralUrbanMappingResponse();
                    ruralUrbanMappingResponse.setDivisionCode(UNKNOWN);
                    ruralUrbanMappingResponse.setStatus(UNKNOWN);
                    ruralUrbanMappingResponse.setDiscomName(UNKNOWN);
                    ruralUrbanMappingResponse.setZoneName(UNKNOWN);
                    ruralUrbanMappingResponse.setCircleName(UNKNOWN);
                    ruralUrbanMappingResponse.setDistrictName(UNKNOWN);
                    ruralUrbanMappingResponse.setDivisionName(UNKNOWN);
                } else {
                    ruralUrbanMappingResponse = ruralUrbanMappingService.getMapping(paymentType.toString(), code);
                }
                if (ruralUrbanMappingResponse != null) {
                    divisionCodeMapping.put(code, ruralUrbanMappingResponse);
                }
            });
        }
        return divisionCodeMapping;
    }

    @RequestMapping(value = "/bill/discom/division", method = RequestMethod.GET)
    public ResponseEntity<?> getBillCollectionByDiscomByDivision(@RequestParam(value = "discom", required = false) String discom,
                                                                 @RequestParam(value = "division", required = false) String division,
                                                                 @RequestParam(value = "van", required = false) String van,
                                                                 @RequestParam(value = "transactionType", required = false) PaymentType transactionType,
                                                                 @RequestParam(value = "startTime", required = false) Long startTime,
                                                                 @RequestParam(value = "endTime", required = false) Long endTime) {
        List<String> vanIds = null;
        if (van != null) {
            Agent queryAgent = new Agent();
            queryAgent.setVan(van);
            Optional<Agent> optAgent = agentRepository.findOne(Example.of(queryAgent));
            if (!optAgent.isPresent()) {
                ServiceMessage serviceMessage = new ServiceMessage();
                serviceMessage.setMessage("No agent/agency exist with van " + van);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceMessage);
            }
            vanIds = new ArrayList<>();
            vanIds.add(van);
            if (optAgent.get().getAgentType() == AgentType.AGENCY) {
                vanIds.addAll(getAgentsForAgency(optAgent.get().getId()));
            }
        }

        List<Transaction> transactions = getPaymentTransactionsByVanIds(vanIds, discom, division, transactionType, null, null, false, startTime, endTime);
        Map<String, List<Transaction>> discomMap = transactions.stream()
                .filter(txn -> txn.getDiscom() != null)
                .collect(Collectors.groupingBy(Transaction::getDiscom));
        Map<String, Map<String, List<Transaction>>> discomDivisionMap = discomMap.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey(),
                        entry -> entry.getValue().stream()
                                .filter(txn -> txn.getDivision() != null)
                                .collect(Collectors.groupingBy(Transaction::getDivision))));
        DiscomDivisionReport discomDivisionReport = new DiscomDivisionReport();
        List<DiscomDivisionReportEntry> entries = new ArrayList<>();
        discomDivisionMap.entrySet().stream().forEach(entry -> {
            entry.getValue().forEach((key, value) -> {
                DiscomDivisionReportEntry discomDivisionReportEntry = new DiscomDivisionReportEntry();
                discomDivisionReportEntry.setDiscom(entry.getKey());
                discomDivisionReportEntry.setDivision(key);
                Set<String> agencies = new HashSet<>();
                Set<String> agents = new HashSet<>();
                value.forEach(txn -> {
                    Optional<Agent> agentOpt = agentRepository.findById(txn.getEntityId());
                    if (agentOpt.isPresent()) {
                        Agent agent = agentOpt.get();
                        if (agent.getAgentType() == AgentType.AGENCY) {
                            agencies.add(agent.getId());
                        } else {
                            agents.add(agent.getId());
                            if (agent.getAgencyId() != null) {
                                Optional<Agent> agencyOpt = agentRepository.findById(agent.getAgencyId());
                                if (agencyOpt.isPresent()) {
                                    agencies.add(agencyOpt.get().getId());
                                }
                            }
                        }
                    }
                });
                int totalAgents = agents.size();
                int totalAgencies = agencies.size();
                discomDivisionReportEntry.setTotalAgents(totalAgents);
                discomDivisionReportEntry.setTotalAgencies(totalAgencies);
                Map<String, List<Transaction>> externalIdTxns = value.stream()
                        .filter(txn -> txn.getExternalId() != null)
                        .collect(Collectors.groupingBy(Transaction::getExternalId));
                int totalExternalAgents = externalIdTxns.size();
                List<Transaction> internalIdTxns = externalIdTxns.get("");
                if (internalIdTxns != null) {
                    totalExternalAgents = totalExternalAgents - 1;
                    Map<String, List<Transaction>> internalAgentAndAgencies = internalIdTxns.stream()
                            .filter(txn -> txn.getEntityId() != null)
                            .collect(Collectors.groupingBy(Transaction::getEntityId));
                    discomDivisionReportEntry.setTotalAgents(internalAgentAndAgencies.size());
                }
                discomDivisionReportEntry.setTotalExternalAgents(totalExternalAgents);
                Map<String, List<Transaction>> billTxns = value.stream()
                        .filter(txn -> txn.getBillId() != null)
                        .collect(Collectors.groupingBy(Transaction::getBillId));
                discomDivisionReportEntry.setTotalBillCount(billTxns.size());
                Double sum = 0d;
                for (Transaction txn : value) {
                    sum += txn.getAmount();
                }
                discomDivisionReportEntry.setTotalBillCollection(sum);
                entries.add(discomDivisionReportEntry);
            });
        });
        entries.sort(Comparator.comparing(DiscomDivisionReportEntry::getDiscom));
        discomDivisionReport.setItems(entries);
        return ResponseEntity.status(HttpStatus.OK).body(discomDivisionReport);
    }

    @RequestMapping(value = "/wallet", method = RequestMethod.GET)
    public ResponseEntity<?> getWalletReport(@RequestParam(value = "van", required = false) String van,
                                             @RequestParam(value = "agentType", required = false) AgentType agentType,
                                             @RequestParam(value = "startTime", required = false) Long startTime,
                                             @RequestParam(value = "endTime", required = false) Long endTime,
                                             @RequestParam(value = "pageSize", required = false) String pageSize,
                                             @RequestParam(value = "nextPageToken", required = false) String nextPageToken) {
        Page p = CommonUtils.getPage(gson, pageSize, nextPageToken);
        final Pageable pageableRequest = PageRequest.of(p.getPage(), p.getSize(), Sort.by("van").descending());

        // 1. Get All Agents
        Query query = new Query();
        if (agentType != null) {
            query.addCriteria(Criteria.where("agentType").is(agentType));
        }
        if (van != null) {
            query.addCriteria(Criteria.where("van").is(van));
        }
        query.with(pageableRequest);
        List<Agent> agents = mongoTemplate.find(query, Agent.class);

        WalletReport report = new WalletReport();
        List<WalletReportEntry> items = new ArrayList<>();

        // 2. Iterate and create response entry
        if (agents != null && !agents.isEmpty()) {
            items = agents.parallelStream().map(agent -> createWalletReportEntry(agent, startTime, endTime)).collect(Collectors.toList());
        }
        if (p.getSize() - items.size() == 0) {
            report.setNextPageToken(CommonUtils.nextPageToken(gson, p));
        }
        report.setItems(items);
        return ResponseEntity.status(HttpStatus.OK).body(report);
    }

    @RequestMapping(value = "/walletDistribution", method = RequestMethod.GET)
    public ResponseEntity<?> getWalletDistributionReport(@RequestParam(value = "agencyVan") String agencyVan,
                                                         @RequestParam(value = "agentVan", required = false) String agentVan,
                                                         @RequestParam(value = "startTime", required = false) Long startTime,
                                                         @RequestParam(value = "endTime", required = false) Long endTime) {
        Agent agencySearch = new Agent();
        agencySearch.setVan(agencyVan);
        agencySearch.setAgentType(AgentType.AGENCY);
        Optional<Agent> agencyOpt = agentRepository.findOne(Example.of(agencySearch));
        if (!agencyOpt.isPresent()) {
            ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("No agency wallet exist with van " + agencyVan);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(serviceMessage);
        }
        Agent agency = agencyOpt.get();
        List<Agent> agents = null;
        if (agentVan != null) {
            Agent agentSearch = new Agent();
            agentSearch.setVan(agentVan);
            agentSearch.setAgentType(AgentType.AGENT);
            agentSearch.setAgencyId(agency.getId());
            Optional<Agent> agentOpt = agentRepository.findOne(Example.of(agentSearch));
            if (!agentOpt.isPresent()) {
                ServiceMessage serviceMessage = new ServiceMessage();
                serviceMessage.setMessage("No agent wallet exist with van " + agentVan);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(serviceMessage);
            }
            agents = Collections.singletonList(agentOpt.get());
        } else {
            agents = getAgentsForAgency(agency.getId(), null, null);
        }
        if (CollectionUtils.isEmpty(agents)) {
            ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setMessage("No agents found for agency with van " + agencyVan);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(serviceMessage);
        }
        Map<String, Agent> agentsMap = agents.stream().collect(Collectors.toMap(Agent::getVan, agent -> agent));
        agentsMap.put(agency.getVan(), agency);
        List<Transaction> transactions = getPaymentTransactionsByVanIds(new ArrayList<>(agentsMap.keySet()), null, null, null, TransactionType.CREDIT, null, true, startTime, endTime);
        Map<String, List<Transaction>> externalIdMap = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getExternalTransactionId));
        List<WalletDistributionReportEntry> items = new ArrayList<>();
        externalIdMap.forEach((key, walletTransactions) -> {
            Optional<Transaction> agentTxn = walletTransactions.stream().filter(txn -> txn != null && !txn.getVanId().equals(agency.getVan())).findAny();
            agentTxn.ifPresent(transaction -> walletTransactions.forEach(txn -> {
                agentsMap.forEach((agentKey, agent) -> {
                    String txVan = txn.getVanId();
                    String agntVan = agent.getVan();
                    if (txVan.equalsIgnoreCase(agntVan)) {
                        String uniqueId = agent.getUniqueId();
                        User user = agent.getUser();
                        StringBuilder builder = new StringBuilder();
                        String agentName = builder.append(user.getFirstName()).append(" ").append(user.getLastName()).toString();
                        WalletDistributionReportEntry walletDistributionReportEntry = createWalletDistributionReport(txn, agency.getVan(), transaction.getVanId(), uniqueId, agentName);
                        items.add(walletDistributionReportEntry);
                    }
                });
            }));
        });
        logger.info("Wallet Distribution: Transactions = {}, Entries = {}", transactions.size(), items.size());
        items.sort(Comparator.comparing(WalletDistributionReportEntry::getTransactionTime).reversed());
        WalletDistributionReport report = new WalletDistributionReport();
        report.setCount(items.size());
        report.setItems(items);
        return ResponseEntity.status(HttpStatus.OK).body(report);
    }

    private WalletDistributionReportEntry createWalletDistributionReport(Transaction transaction, String agencyVan, String agentVan, String uniqueId, String agentName) {
        // agents.stream().filter(agent -> agent == 0)
        WalletDistributionReportEntry entry = new WalletDistributionReportEntry();
        entry.setTransactionId(transaction.getExternalTransactionId());
        entry.setAgencyVan(agencyVan);
        entry.setAgentVan(agentVan);
        entry.setTransactionType(transaction.getActivity());
        entry.setAmount(transaction.getAmount());
        entry.setTransactionTime(transaction.getTransactionTime());
        entry.setPaymentType(transaction.getTransactionType());
        entry.setUniqueId(uniqueId);
        entry.setAgentName(agentName);
        return entry;
    }

    private WalletReportEntry createWalletReportEntry(Agent agent, Long startTime, Long endTime) {
        WalletReportEntry entry = new WalletReportEntry();
        entry.setVan(agent.getVan());
        entry.setAgentType(agent.getAgentType());
        entry.setName(getAgentOrAgencyName(agent));
        entry.setCurrentBalanceAmount(getBalance(agent.getVan()));
        entry.setTotalRechargeAmount(getCreditTransactions(agent.getVan(), startTime, endTime));
        return entry;
    }

    private String getAgentOrAgencyName(Agent agent) {
        if (agent.getAgentType() == AgentType.AGENCY) {
            return agent.getAgencyName();
        } else {
            User user = agent.getUser();
            StringBuilder builder = new StringBuilder();
            return builder.append(user.getFirstName()).append(" ").append(user.getLastName()).toString();
        }
    }

    private Double getCreditTransactions(String van, Long startTime, Long endTime) {
        logger.info("Getting transactions for van {}, startTime {}, endTime {}", van, startTime, endTime);
        Query query = new Query();
        List<Criteria> queryConditions = new ArrayList<>();
        if (startTime != null || endTime != null) {
            Criteria range = Criteria.where("transactionTime");
            if (startTime != null) {
                range.gte(Instant.ofEpochMilli(startTime));
            }
            if (endTime != null) {
                range.lte(Instant.ofEpochMilli(endTime));
            }
            queryConditions.add(range);
        }
        queryConditions.add(Criteria.where("vanId").is(van));
        queryConditions.add(Criteria.where("activity").is(TransactionType.CREDIT.name()));
        Criteria criteria = new Criteria();
        criteria.andOperator(queryConditions.stream().toArray(Criteria[]::new));
        query.addCriteria(criteria);
        List<Transaction> transactions = mongoTemplate.find(query, Transaction.class);
        Double creditTransactions = 0d;
        if (transactions != null) {
            creditTransactions += transactions.stream().mapToDouble(Transaction::getAmount).sum();
        }
        return creditTransactions;
    }

    private List<Transaction> getPaymentTransactionsByVanIds(List<String> vanIds, String discom, String division,
                                                             PaymentType paymentType, TransactionType transactionType,
                                                             String consumerId, boolean walletTransfer,
                                                             Long startTime, Long endTime) {
        Query query = new Query();
        List<Criteria> queryConditions = new ArrayList<>();
        if (vanIds != null && !vanIds.isEmpty()) {
            queryConditions.add(Criteria.where("vanId").in(vanIds));
        }
        if (discom != null) {
            queryConditions.add(Criteria.where("discom").regex("^" + discom + "$", "i"));
        }
        if (division != null) {
            queryConditions.add(Criteria.where("division").regex("^" + division + "$", "i"));
        }
        if (consumerId != null) {
            queryConditions.add(Criteria.where("consumerId").regex("^" + consumerId + "$", "i"));
        }
        if (startTime != null || endTime != null) {
            Criteria range = Criteria.where("transactionTime");
            if (startTime != null) {
                range.gte(Instant.ofEpochMilli(startTime));
            }
            if (endTime != null) {
                range.lte(Instant.ofEpochMilli(endTime));
            }
            queryConditions.add(range);
        }
        if (transactionType != null) {
            queryConditions.add(Criteria.where("activity").is(transactionType.name()));
        }
        queryConditions.add(Criteria.where("vanId").ne(Constants.UPPCL_VAN));
        if (paymentType != null) {
            queryConditions.add(Criteria.where("transactionType").in(Arrays.asList(paymentType.toString())));
        } else {
            if (walletTransfer) {
                queryConditions.add(Criteria.where("transactionType").in(Arrays.asList(SourceType.WALLET.toString())));
            } else {
                queryConditions.add(Criteria.where("transactionType").in(Arrays.asList(PaymentType.NON_RAPDRP.toString(),
                        PaymentType.RAPDRP.toString())));
            }
        }
        Criteria criteria = new Criteria();
        criteria.andOperator(queryConditions.stream().toArray(Criteria[]::new));
        query.addCriteria(criteria);
        List<Transaction> transactions = mongoTemplate.find(query, Transaction.class);
        return transactions;
    }


    private List<Transaction> getWalletRecharge(Set<String> vanIds, Long startTime, Long endTime) {
        Query query = new Query();
        List<Criteria> queryConditions = new ArrayList<>();
        if (vanIds != null && !vanIds.isEmpty()) {
            queryConditions.add(Criteria.where("vanId").in(vanIds));
        }
        if (startTime != null || endTime != null) {
            Criteria range = Criteria.where("transactionTime");
            if (startTime != null) {
                range.gte(Instant.ofEpochMilli(startTime));
            }
            if (endTime != null) {
                range.lte(Instant.ofEpochMilli(endTime));
            }
            queryConditions.add(range);
        }
        queryConditions.add(Criteria.where("vanId").ne(Constants.UPPCL_VAN));
        queryConditions.add(Criteria.where("activity").in(Arrays.asList(TransactionType.CREDIT.toString())));
        Criteria criteria = new Criteria();
        criteria.andOperator(queryConditions.stream().toArray(Criteria[]::new));
        query.addCriteria(criteria);
        List<Transaction> transactions = mongoTemplate.find(query, Transaction.class);
        return transactions;
    }

    private List<Transaction> getPaymentTransactions(Long startTime, Long endTime) {
        Query query = new Query();
        List<Criteria> queryConditions = new ArrayList<>();
        if (startTime != null || endTime != null) {
            Criteria range = Criteria.where("transactionTime");
            if (startTime != null) {
                range.gte(Instant.ofEpochMilli(startTime));
            }
            if (endTime != null) {
                range.lte(Instant.ofEpochMilli(endTime));
            }
            queryConditions.add(range);
        }
        queryConditions.add(Criteria.where("vanId").ne(Constants.UPPCL_VAN));
        queryConditions.add(Criteria.where("transactionType").in(Arrays.asList(PaymentType.NON_RAPDRP.toString(),
                PaymentType.RAPDRP.toString())));
        Criteria criteria = new Criteria();
        criteria.andOperator(queryConditions.stream().toArray(Criteria[]::new));
        query.addCriteria(criteria);
        List<Transaction> transactions = mongoTemplate.find(query, Transaction.class);
        return transactions;
    }

    private Agent getAgentByVan(String van) {
        Query query = new Query();
        query.addCriteria(Criteria.where("van").is(van));
        return mongoTemplate.findOne(query, Agent.class);
    }

    private String getAgentOrAgencyName(String van) {
        Agent agent = getAgentByVan(van);
        if (agent != null) {
            return getAgentOrAgencyName(agent);
        } else {
            return van;
        }
    }

    private long findAgentsForAgency(String agencyId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("agentType").is(AgentType.AGENT));
        query.addCriteria(Criteria.where("agencyId").is(agencyId));
        return mongoTemplate.count(query, Agent.class);
    }

    private Set<String> getAgentsForAgency(String agencyId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("agentType").is(AgentType.AGENT));
        query.addCriteria(Criteria.where("agencyId").is(agencyId));
        List<Agent> agents = mongoTemplate.find(query, Agent.class);
        return agents.parallelStream().map(subAgent -> subAgent.getVan()).collect(Collectors.toSet());
    }

    private List<Agent> getAgentsForAgency(String agencyId, Long startTime, Long endTime) {
        Query query = new Query();
        query.addCriteria(Criteria.where("agentType").is(AgentType.AGENT));
        query.addCriteria(Criteria.where("agencyId").is(agencyId));
        if (startTime != null || endTime != null) {
            Criteria range = Criteria.where("createdAt");
            if (startTime != null) {
                range.gte(Instant.ofEpochMilli(startTime));
            }
            if (endTime != null) {
                range.lte(Instant.ofEpochMilli(endTime));
            }
            query.addCriteria(range);
        }
        return mongoTemplate.find(query, Agent.class);
    }

    private String getTime(long longTime) {
        Date date = new Date(longTime);
        return JDF.format(date);
    }

    private Double getBalance(String vanId) {
        Optional<Wallet> walletOpt = walletRepository.findById(vanId);
        if (walletOpt.isPresent()) {
            BigDecimal bd = new BigDecimal(walletOpt.get().getBalance()).setScale(2, RoundingMode.HALF_UP);
            return bd.doubleValue();
        }
        return 0d;
    }

    private void getUrbanMappings(List<Transaction> transactions, Map<String, RuralUrbanMappingResponse> urbanMapping,
                                  Map<RuralUrbanMappingResponse, Set<Transaction>> mappings) {
        if (!CollectionUtils.isEmpty(transactions)) {
            // Get Urban Mappings
            Map<Optional<String>, List<Transaction>> urbanTxns = transactions.stream()
                    .filter(txn -> PaymentType.RAPDRP.toString().equalsIgnoreCase(txn.getTransactionType()))
                    .collect(Collectors.groupingBy(txn -> Optional.ofNullable(txn.getDivisionCode())));
            urbanTxns.forEach((key, txns) -> {
                String code = key.orElse(UNKNOWN);
                RuralUrbanMappingResponse ruralUrbanMappingResponse = urbanMapping.get(code);
                Set<Transaction> mappingTxns = mappings.get(ruralUrbanMappingResponse);
                if (mappingTxns == null) {
                    mappingTxns = new HashSet<>();
                }
                mappingTxns.addAll(txns);
                mappings.put(ruralUrbanMappingResponse, mappingTxns);
            });
        }
    }

    private void getRuralMappings(List<Transaction> transactions, Map<String, RuralUrbanMappingResponse> ruralMapping,
                                  Map<RuralUrbanMappingResponse, Set<Transaction>> mappings) {
        if (!CollectionUtils.isEmpty(transactions)) {
            // Get Rural Mappings
            Map<Optional<String>, List<Transaction>> ruralTxns = transactions.stream()
                    .filter(txn -> PaymentType.NON_RAPDRP.toString().equalsIgnoreCase(txn.getTransactionType()))
                    .collect(Collectors.groupingBy(txn -> Optional.ofNullable(txn.getDivisionCode())));
            ruralTxns.forEach((key, txns) -> {
                String code = key.orElse(null);
                RuralUrbanMappingResponse ruralUrbanMappingResponse = ruralMapping.get(code);
                Set<Transaction> mappingTxns = mappings.get(ruralUrbanMappingResponse);
                if (mappingTxns == null) {
                    mappingTxns = new HashSet<>();
                }
                mappingTxns.addAll(txns);
                mappings.put(ruralUrbanMappingResponse, mappingTxns);
            });
        }
    }


    @RequestMapping(value = "/bill/agencyV2", method = RequestMethod.GET)
    public ResponseEntity<?> getBillCollectionForAgencyV2(@RequestParam(value = "discom", required = false) String discom,
                                                          @RequestParam(value = "division", required = false) String division,
                                                          @RequestParam(value = "van", required = false) String van,
                                                          @RequestParam(value = "transactionType", required = false) PaymentType transactionType,
                                                          @RequestParam(value = "consumerId", required = false) String consumerId,
                                                          @RequestParam(value = "startTime", required = true) Long startTime,
                                                          @RequestParam(value = "endTime", required = true) Long endTime) {

        MongoDatabase database = mongoClient.getDatabase("wallet");
        MongoCollection<Document> agentCollection = database.getCollection("agent");
        List<Object> documents = new ArrayList<>();
        AggregateIterable<Document> result = null;
        Document matchResult = new Document();
        matchResult.append("vanId", new Document("$ne", "UPPCL"));
        // matchResult.append("entityType", "AGENT");
        if (discom != null) {
            matchResult.append("discom", Pattern.compile("^" + discom));
        }
        if (division != null) {
            matchResult.append("division", division);
        }
        if (van != null) {
            //eq("van", "UPCA8554408791")
            Bson projectionFields = Projections.fields(
                    Projections.include("_id"));
            // Projections.excludeId());
            Document agencyId = agentCollection.find(eq("van", van))
                    .projection(projectionFields)
                    // .sort(Sorts.descending("imdb.rating"))
                    .first();
            matchResult.append("agencyId", agencyId.get("_id"));
        }
        if (transactionType != null) {
            matchResult.append("transactionType", transactionType.name());
        } else {
            matchResult.append("$or", Arrays.asList(new Document("transactionType", transactionType.RAPDRP.name()),
                    new Document("transactionType", transactionType.NON_RAPDRP.name())));
        }
        if (consumerId != null) {
            matchResult.append("consumerId", consumerId);
        }
        // if(discom != null && division != null && van!=null && transactionType !=null && consumerId !=null) {
        result = database
                .getCollection("transaction").aggregate(Arrays.asList(new Document("$match",
                                matchResult
                                        .append("$and", Arrays.asList(new Document("transactionTime",
                                                        new Document("$gte",
                                                                new Date(startTime))),
                                                new Document("transactionTime",
                                                        new Document("$lte",
                                                                new Date(endTime)))))),
                        new Document("$lookup",
                                new Document("from", "agent")
                                        .append("localField", "entityId")
                                        .append("foreignField", "_id")
                                        .append("as", "agent")),
                        new Document("$unwind",
                                new Document("path", "$agent")),
                        new Document("$lookup",
                                new Document("from", "user")
                                        .append("localField", "agent.user.$id")
                                        .append("foreignField", "_id")
                                        .append("as", "agentDetails")),
                        new Document("$unwind",
                                new Document("path", "$agentDetails")),
                        new Document("$lookup",
                                new Document("from", "agent")
                                        .append("localField", "agencyId")
                                        .append("foreignField", "_id")
                                        .append("as", "agency")),
                        new Document("$unwind",
                                new Document("path", "$agency")
                                        .append("preserveNullAndEmptyArrays", true)),
                        new Document("$project",
                                new Document("externalTransactionId", 1L)
                                        .append("vanId", 1L)
                                        .append("agentName",
                                                new Document("$concat", Arrays.asList("$agentDetails.firstName", " ", "$agentDetails.lastName")))
                                        .append("amount", 1L)
                                        .append("transactionType", 1L)
                                        .append("transactionTime", 1L)
                                        .append("activity", 1L)
                                        .append("mobile", 1L)
                                        .append("discom", 1L)
                                        .append("division", 1L)
                                        .append("billId", 1L)
                                        .append("consumerId", 1L)
                                        .append("divisionCode", 1L)
                                        .append("transactionState", 1L)
                                        .append("agencyVan", 1L)
                                        .append("agencyName", "$agency.agencyName"))));


        // }
        for (Document dbObject : result) {
            // System.out.println(dbObject);
            if (dbObject.get("agencyName") == null) {
                dbObject.put("agencyName", dbObject.get("agentName"));
                dbObject.remove("agentName");
            }
            SimpleDateFormat sdf2 = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss a");
            sdf2.setTimeZone(TimeZone.getTimeZone("IST"));
            String dateStr = sdf2.format(dbObject.get("transactionTime")); // Output: 15-02-2014 10:48:08 AM
            dbObject.put("transactionTime", dateStr.toUpperCase());
            dbObject.put("transactionId", dbObject.get("externalTransactionId"));
            dbObject.remove("externalTransactionId");
            documents.add(dbObject);
        }
        return ResponseEntity.ok(documents);
    }

    @RequestMapping(value = "/bill/divisionV2", method = RequestMethod.GET)
    public ResponseEntity<?> getDivisionReportV2(@RequestParam(value = "discom", required = false) String discom,
                                                 @RequestParam(value = "division", required = false) String division,
                                                 @RequestParam(value = "van", required = false) String van,
                                                 @RequestParam(value = "transactionType", required = false) PaymentType transactionType,
                                                 @RequestParam(value = "consumerId", required = false) String consumerId,
                                                 @RequestParam(value = "startTime", required = true) Long startTime,
                                                 @RequestParam(value = "tillDateStartTime", required = true) Long tillDateStartTime,
                                                 @RequestParam(value = "endTime", required = true) Long endTime) throws ParseException {

        Document filter = new Document();
        if (discom != null)
            filter.put("discom", discom);
        if (division != null)
            filter.put("division", division);
        if (van != null)
            filter.put("van", van);
        if (transactionType != null)
            filter.put("transactionType", transactionType.name());
        if (consumerId != null)
            filter.put("consumerId", consumerId);
        filter.put("startTime", startTime);
        filter.put("endTime", endTime);
        // DateFormat simple = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // String fetchedDate = simple.format(new Date(endTime));
        // SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // Date date = sdf.parse(fetchedDate);
        // Long startMilliseconds = date.getTime();
        // Long endMilliseconds = date.getTime()+ 86399000; // 23.59.59 Hours
        filter.put("tillDateStartTime", tillDateStartTime);
        filter.put("tillDateEndTime", endTime);
        List<Object> documents = new ArrayList<>();
        AggregateIterable<Document> result = (AggregateIterable<Document>) divisionService.findData2(filter);
        for (Document dbObject : result) {
            if (dbObject.get("discom") != null) {
                dbObject.put("discom", dbObject.get("discom").toString().toUpperCase());
            }
            documents.add(dbObject);
        }
        return ResponseEntity.ok(documents);
    }

    @RequestMapping(value = "/agency-wise/commission", method = RequestMethod.GET)
    public ResponseEntity<?> getAgencyWiseCommission(@RequestParam(value = "discom", required = false) String discom,
                                                 @RequestParam(value = "division", required = false) String division,
                                                 @RequestParam(value = "van", required = false) String van,
                                                 // @RequestParam(value = "transactionType", required = false) PaymentType transactionType,
                                                 @RequestParam(value = "consumerId", required = false) String consumerId,
                                                 @RequestParam(value = "startTime", required = true) Long startTime,
                                                 // @RequestParam(value = "tillDateStartTime", required = true) Long tillDateStartTime,
                                                 @RequestParam(value = "endTime", required = true) Long endTime) throws ParseException {

        Document filter = new Document();
        if (discom != null)
            filter.put("discom", discom);
        if (division != null)
            filter.put("division", division);
        if (van != null)
            filter.put("van", van);
        if (consumerId != null)
            filter.put("consumerId", consumerId);
        filter.put("startTime", startTime);
        filter.put("endTime", endTime);
        List<Object> documents = new ArrayList<>();
        AggregateIterable<Document> result = (AggregateIterable<Document>) divisionService.getCommissionWiseReport(filter);
        for (Document dbObject : result) {
            if (dbObject.get("discom") != null) {
                dbObject.put("discom", dbObject.get("discom").toString().toUpperCase());
            }
            documents.add(dbObject);
        }
        return ResponseEntity.ok(documents);
    }

    
    private Agent findAgentByVanId(String vanId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("van").is(vanId));
        query.addCriteria(Criteria.where("agentType").is(AgentType.AGENT.name()));
        return mongoTemplate.findOne(query, Agent.class);
    }
}
