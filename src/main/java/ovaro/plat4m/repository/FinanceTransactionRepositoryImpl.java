package ovaro.plat4m.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import ovaro.plat4m.service.dto.FinanceTransactionRowDTO;

@Repository
public class FinanceTransactionRepositoryImpl implements FinanceTransactionRepositoryCustom {

    private static final String BASE_CTE =
        "WITH txn_rows AS (" +
        " SELECT " +
        "  f.account_id as accountId, " +
        "  f.amount as amount, " +
        "  f.category_id as categoryId, " +
        "  c.name as categoryName, " +
        "  f.cleared as cleared, " +
        "  f.date as date, " +
        "  f.id as id, " +
        "  f.investment as investment, " +
        "  CAST(f.investment_activity_type as text) as investmentActivityType, " +
        "  f.investment_activity_type as investmentActivityTypeId, " +
        "  f.master_guid as masterGuid, " +
        "  f.memo as memo, " +
        "  f.number as number, " +
        "  c.parent_category_id as parentCategoryId, " +
        "  pc.name as parentCategoryName, " +
        "  f.payee_id as payeeId, " +
        "  f.payee_name as payeeName, " +
        "  f.price as price, " +
        "  f.quantity as quantity, " +
        "  f.reconciled as reconciled, " +
        "  f.recurring as recurring, " +
        "  sum(f.amount) over (order by f.date asc, f.number asc nulls last, f.id asc rows between unbounded preceding and current row) as runningBalance, " +
        "  f.security_id as securityId, " +
        "  f.split_child as splitChild, " +
        "  f.split_parent as splitParent, " +
        "  f.statement_id as statementId, " +
        "  f.status_flag as statusFlag, " +
        "  f.transfer as transfer, " +
        "  f.transfer_to as transferTo, " +
        "  f.transferred_account_id as transferredAccountId, " +
        "  f.voided as voided, " +
        "  f.who_id as whoId, " +
        "  w.name as whoName, " +
        "  coalesce(tag_info.tags_json, '[]') as tagsJson, " +
        "  tag_info.tags_display as tagsDisplay, " +
        "  CASE " +
        "   WHEN f.split_parent THEN 'Split / Multiple Categories' " +
        "   WHEN c.name IS NOT NULL AND pc.name IS NOT NULL THEN c.name || ': ' || pc.name " +
        "   ELSE c.name " +
        "  END as displayCategory, " +
        "  CASE WHEN f.amount < 0 THEN f.amount END as payment, " +
        "  CASE WHEN f.amount > 0 THEN f.amount END as deposit " +
        " FROM fin_transaction f " +
        " LEFT JOIN fin_category c ON uuid(c.id) = uuid(f.category_id) " +
        " LEFT JOIN fin_category pc ON uuid(pc.id) = uuid(c.parent_category_id) " +
        " LEFT JOIN fin_category w ON uuid(w.id) = uuid(f.who_id) " +
        " LEFT JOIN (" +
        "   SELECT ftt.transaction_id, " +
        "          coalesce(json_agg(ft.name ORDER BY coalesce(ftt.sort_order, 0), lower(ft.name)) FILTER (WHERE ft.name IS NOT NULL), '[]'::json)::text as tags_json, " +
        "          string_agg(ft.name, ', ' ORDER BY coalesce(ftt.sort_order, 0), lower(ft.name)) as tags_display " +
        "   FROM fin_transaction_tag ftt " +
        "   INNER JOIN fin_tag ft ON uuid(ft.id) = uuid(ftt.tag_id) " +
        "   GROUP BY ftt.transaction_id" +
        " ) tag_info ON uuid(tag_info.transaction_id) = uuid(f.id) " +
        " WHERE f.account_id = :account_id and f.user_guid = :user_guid and not voided and not split_child and not recurring" +
        ")";

    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    public FinanceTransactionRepositoryImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Page<FinanceTransactionRowDTO> findTransactionRows(String userGuid, String accountId, Pageable pageable, String filterModel) {
        StringBuilder whereClause = new StringBuilder(" WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        params.put("user_guid", userGuid);
        params.put("account_id", accountId);

        appendFilterClauses(whereClause, params, filterModel);

        String selectSql =
            BASE_CTE +
            " SELECT accountId, amount, categoryId, categoryName, cleared, date, id, investment, investmentActivityType, investmentActivityTypeId, " +
            " masterGuid, memo, number, parentCategoryId, parentCategoryName, payeeId, payeeName, price, quantity, reconciled, recurring, " +
            " runningBalance, securityId, splitChild, splitParent, statementId, statusFlag, transfer, transferTo, transferredAccountId, voided, whoId, whoName, tagsJson, tagsDisplay" +
            " FROM txn_rows" +
            whereClause +
            buildOrderBy(pageable);
        String countSql = BASE_CTE + " SELECT count(*) FROM txn_rows" + whereClause;

        Query dataQuery = entityManager.createNativeQuery(selectSql);
        Query countQuery = entityManager.createNativeQuery(countSql);
        params.forEach((key, value) -> {
            dataQuery.setParameter(key, value);
            countQuery.setParameter(key, value);
        });

        dataQuery.setFirstResult((int) pageable.getOffset());
        dataQuery.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQuery.getResultList();
        List<FinanceTransactionRowDTO> content = rows.stream().map(this::mapRow).toList();
        Number total = (Number) countQuery.getSingleResult();
        return new PageImpl<>(content, pageable, total.longValue());
    }

    @Override
    public FinanceTransactionRowDTO findTransactionRowById(String userGuid, String accountId, UUID transactionId) {
        Query query = entityManager.createNativeQuery(
            BASE_CTE +
                " SELECT accountId, amount, categoryId, categoryName, cleared, date, id, investment, investmentActivityType, investmentActivityTypeId, " +
                " masterGuid, memo, number, parentCategoryId, parentCategoryName, payeeId, payeeName, price, quantity, reconciled, recurring, " +
                " runningBalance, securityId, splitChild, splitParent, statementId, statusFlag, transfer, transferTo, transferredAccountId, voided, whoId, whoName, tagsJson, tagsDisplay" +
                " FROM txn_rows WHERE uuid(id) = uuid(:transaction_id)"
        );
        query.setParameter("user_guid", userGuid);
        query.setParameter("account_id", accountId);
        query.setParameter("transaction_id", transactionId.toString());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        if (rows.isEmpty()) {
            return null;
        }

        return mapRow(rows.get(0));
    }

    private FinanceTransactionRowDTO mapRow(Object[] row) {
        FinanceTransactionRowDTO dto = new FinanceTransactionRowDTO();
        dto.setAccountId(asString(row[0]));
        dto.setAmount(asBigDecimal(row[1]));
        dto.setCategoryId(asString(row[2]));
        dto.setCategoryName(asString(row[3]));
        dto.setCleared(asBoolean(row[4]));
        dto.setDate(asLocalDate(row[5]));
        dto.setId(asString(row[6]));
        dto.setInvestment(asBoolean(row[7]));
        dto.setInvestmentActivityType(asString(row[8]));
        dto.setInvestmentActivityTypeId(asInteger(row[9]));
        dto.setMasterGuid(asString(row[10]));
        dto.setMemo(asString(row[11]));
        dto.setNumber(asInteger(row[12]));
        dto.setParentCategoryId(asString(row[13]));
        dto.setParentCategoryName(asString(row[14]));
        dto.setPayeeId(asString(row[15]));
        dto.setPayeeName(asString(row[16]));
        dto.setPrice(asDouble(row[17]));
        dto.setQuantity(asDouble(row[18]));
        dto.setReconciled(asBoolean(row[19]));
        dto.setRecurring(asBoolean(row[20]));
        dto.setRunningBalance(asBigDecimal(row[21]));
        dto.setSecurityId(asString(row[22]));
        dto.setSplitChild(asBoolean(row[23]));
        dto.setSplitParent(asBoolean(row[24]));
        dto.setStatementId(asString(row[25]));
        dto.setStatusFlag(asInteger(row[26]));
        dto.setTransfer(asBoolean(row[27]));
        dto.setTransferTo(asBoolean(row[28]));
        dto.setTransferredAccountId(asString(row[29]));
        dto.setVoided(asBoolean(row[30]));
        dto.setWhoId(asString(row[31]));
        dto.setWhoName(asString(row[32]));
        dto.setTags(parseStringList(row[33]));
        dto.setTagsDisplay(asString(row[34]));
        return dto;
    }

    private void appendFilterClauses(StringBuilder whereClause, Map<String, Object> params, String filterModel) {
        if (filterModel == null || filterModel.isBlank()) {
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(filterModel);
            if (root == null || !root.isObject()) {
                return;
            }

            int index = 0;
            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String clause = buildColumnClause(entry.getKey(), entry.getValue(), params, "f" + index);
                if (clause != null && !clause.isBlank()) {
                    whereClause.append(" AND ").append(clause);
                }
                index++;
            }
        } catch (Exception ignored) {
            // Ignore malformed filter payloads and fall back to the unfiltered query.
        }
    }

    private String buildColumnClause(String columnId, JsonNode node, Map<String, Object> params, String keyPrefix) {
        ColumnSpec columnSpec = getColumnSpec(columnId);
        if (columnSpec == null || node == null || node.isNull()) {
            return null;
        }

        if (node.has("conditions") && node.get("conditions").isArray() && node.get("conditions").size() > 0) {
            List<String> clauses = new ArrayList<>();
            int index = 0;
            for (JsonNode condition : node.get("conditions")) {
                String clause = buildLeafClause(columnSpec, condition, params, keyPrefix + "_" + index);
                if (clause != null && !clause.isBlank()) {
                    clauses.add(clause);
                }
                index++;
            }
            if (clauses.isEmpty()) {
                return null;
            }
            String operator = "OR".equalsIgnoreCase(node.path("operator").asText()) ? " OR " : " AND ";
            return "(" + String.join(operator, clauses) + ")";
        }

        if (node.has("condition1")) {
            List<String> clauses = new ArrayList<>();
            String clause1 = buildLeafClause(columnSpec, node.get("condition1"), params, keyPrefix + "_0");
            String clause2 = buildLeafClause(columnSpec, node.get("condition2"), params, keyPrefix + "_1");
            if (clause1 != null && !clause1.isBlank()) {
                clauses.add(clause1);
            }
            if (clause2 != null && !clause2.isBlank()) {
                clauses.add(clause2);
            }
            if (clauses.isEmpty()) {
                return null;
            }
            String operator = "OR".equalsIgnoreCase(node.path("operator").asText()) ? " OR " : " AND ";
            return "(" + String.join(operator, clauses) + ")";
        }

        return buildLeafClause(columnSpec, node, params, keyPrefix);
    }

    private String buildLeafClause(ColumnSpec columnSpec, JsonNode node, Map<String, Object> params, String keyPrefix) {
        if (node == null || node.isNull()) {
            return null;
        }

        return switch (columnSpec.kind) {
            case TEXT -> buildTextClause(columnSpec.expression, node, params, keyPrefix);
            case NUMBER -> buildNumberClause(columnSpec.expression, node, params, keyPrefix);
            case DATE -> buildDateClause(columnSpec.expression, node, params, keyPrefix);
        };
    }

    private String buildTextClause(String expression, JsonNode node, Map<String, Object> params, String keyPrefix) {
        String type = node.path("type").asText();
        switch (type) {
            case "blank":
                return "(" + expression + " IS NULL OR trim(" + expression + ") = '')";
            case "notBlank":
                return "(" + expression + " IS NOT NULL AND trim(" + expression + ") <> '')";
            default:
                String value = node.path("filter").asText(null);
                if (value == null || value.isBlank()) {
                    return null;
                }
                String param = keyPrefix + "_text";
                return switch (type) {
                    case "equals" -> {
                        params.put(param, value.toLowerCase());
                        yield "lower(coalesce(" + expression + ", '')) = :" + param;
                    }
                    case "notEqual" -> {
                        params.put(param, value.toLowerCase());
                        yield "lower(coalesce(" + expression + ", '')) <> :" + param;
                    }
                    case "startsWith" -> {
                        params.put(param, value.toLowerCase() + "%");
                        yield "lower(coalesce(" + expression + ", '')) like :" + param;
                    }
                    case "endsWith" -> {
                        params.put(param, "%" + value.toLowerCase());
                        yield "lower(coalesce(" + expression + ", '')) like :" + param;
                    }
                    case "notContains" -> {
                        params.put(param, "%" + value.toLowerCase() + "%");
                        yield "lower(coalesce(" + expression + ", '')) not like :" + param;
                    }
                    case "contains" -> {
                        params.put(param, "%" + value.toLowerCase() + "%");
                        yield "lower(coalesce(" + expression + ", '')) like :" + param;
                    }
                    default -> null;
                };
        }
    }

    private String buildNumberClause(String expression, JsonNode node, Map<String, Object> params, String keyPrefix) {
        String type = node.path("type").asText();
        switch (type) {
            case "blank":
                return expression + " IS NULL";
            case "notBlank":
                return expression + " IS NOT NULL";
            default:
                if (!node.hasNonNull("filter")) {
                    return null;
                }
                String param = keyPrefix + "_number";
                params.put(param, node.path("filter").decimalValue());
                return switch (type) {
                    case "equals" -> expression + " = :" + param;
                    case "notEqual" -> expression + " <> :" + param;
                    case "greaterThan" -> expression + " > :" + param;
                    case "greaterThanOrEqual" -> expression + " >= :" + param;
                    case "lessThan" -> expression + " < :" + param;
                    case "lessThanOrEqual" -> expression + " <= :" + param;
                    case "inRange" -> {
                        if (!node.hasNonNull("filterTo")) {
                            yield null;
                        }
                        String paramTo = keyPrefix + "_number_to";
                        params.put(paramTo, node.path("filterTo").decimalValue());
                        yield "(" + expression + " >= :" + param + " AND " + expression + " <= :" + paramTo + ")";
                    }
                    default -> null;
                };
        }
    }

    private String buildDateClause(String expression, JsonNode node, Map<String, Object> params, String keyPrefix) {
        String type = node.path("type").asText();
        if ("blank".equals(type)) {
            return expression + " IS NULL";
        }
        if ("notBlank".equals(type)) {
            return expression + " IS NOT NULL";
        }
        String dateFrom = node.path("dateFrom").asText(null);
        if (dateFrom == null || dateFrom.isBlank()) {
            return null;
        }

        String param = keyPrefix + "_date";
        params.put(param, LocalDate.parse(dateFrom));
        return switch (type) {
            case "equals" -> expression + " = :" + param;
            case "notEqual" -> expression + " <> :" + param;
            case "greaterThan" -> expression + " > :" + param;
            case "greaterThanOrEqual" -> expression + " >= :" + param;
            case "lessThan" -> expression + " < :" + param;
            case "lessThanOrEqual" -> expression + " <= :" + param;
            case "inRange" -> {
                String dateTo = node.path("dateTo").asText(null);
                if (dateTo == null || dateTo.isBlank()) {
                    yield null;
                }
                String paramTo = keyPrefix + "_date_to";
                params.put(paramTo, LocalDate.parse(dateTo));
                yield "(" + expression + " >= :" + param + " AND " + expression + " <= :" + paramTo + ")";
            }
            default -> null;
        };
    }

    private String buildOrderBy(Pageable pageable) {
        List<String> orderClauses = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            String expression = switch (order.getProperty()) {
                case "date" -> "date";
                case "payeeName" -> "payeeName";
                case "categoryName", "displayCategory" -> "displayCategory";
                case "memo" -> "memo";
                case "amount" -> "amount";
                case "payment" -> "payment";
                case "deposit" -> "deposit";
                case "runningBalance" -> "runningBalance";
                case "whoName" -> "whoName";
                case "tagsDisplay" -> "tagsDisplay";
                default -> null;
            };

            if (expression != null) {
                orderClauses.add(expression + (order.isAscending() ? " ASC" : " DESC"));
            }
        }

        if (orderClauses.isEmpty()) {
            return " ORDER BY date DESC, number DESC";
        }

        return " ORDER BY " + String.join(", ", orderClauses) + ", number DESC";
    }

    private ColumnSpec getColumnSpec(String columnId) {
        return switch (columnId) {
            case "date" -> new ColumnSpec("date", ColumnKind.DATE);
            case "payee" -> new ColumnSpec("payeeName", ColumnKind.TEXT);
            case "memo" -> new ColumnSpec("memo", ColumnKind.TEXT);
            case "category" -> new ColumnSpec("displayCategory", ColumnKind.TEXT);
            case "who" -> new ColumnSpec("whoName", ColumnKind.TEXT);
            case "tags" -> new ColumnSpec("tagsDisplay", ColumnKind.TEXT);
            case "amount" -> new ColumnSpec("amount", ColumnKind.NUMBER);
            case "payment" -> new ColumnSpec("payment", ColumnKind.NUMBER);
            case "deposit" -> new ColumnSpec("deposit", ColumnKind.NUMBER);
            case "runningBalance" -> new ColumnSpec("runningBalance", ColumnKind.NUMBER);
            default -> null;
        };
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        return value == null ? null : new BigDecimal(value.toString());
    }

    private Boolean asBoolean(Object value) {
        return value == null ? null : (Boolean) value;
    }

    private Integer asInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private Double asDouble(Object value) {
        return value == null ? null : ((Number) value).doubleValue();
    }

    private LocalDate asLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        return value == null ? null : LocalDate.parse(value.toString());
    }

    private List<String> parseStringList(Object value) {
        if (value == null) {
            return List.of();
        }

        try {
            JsonNode node = objectMapper.readTree(value.toString());
            if (!node.isArray()) {
                return List.of();
            }

            List<String> result = new ArrayList<>();
            for (JsonNode child : node) {
                String text = child.asText(null);
                if (text != null && !text.isBlank()) {
                    result.add(text);
                }
            }
            return result;
        } catch (Exception e) {
            return List.of();
        }
    }

    private record ColumnSpec(String expression, ColumnKind kind) {}

    private enum ColumnKind {
        TEXT,
        NUMBER,
        DATE,
    }
}
