package databreeze.enums;

/**
 * Permission theo action, không fix cứng logic nghiệp vụ theo từng role trong service.
 * Role chỉ là nhóm quyền; service gọi requirePermission(...) để kiểm tra.
 */
public enum WorkspacePermission {
    READ_WORKSPACE,
    IMPORT_DATA,
    MANAGE_STORES,
    MANAGE_MEMBERS,
    MANAGE_FINANCIAL_DATA,
    MANAGE_BILLING,
    DELETE_WORKSPACE
}
