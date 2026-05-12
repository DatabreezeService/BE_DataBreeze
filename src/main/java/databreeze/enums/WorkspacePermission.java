package databreeze.enums;

/**
 * Permission cấp workspace.
 *
 * Role không nên được check trực tiếp trong từng service nghiệp vụ.
 * Service nên check theo permission/action.
 *
 * Ví dụ:
 * - ETL service chỉ cần check IMPORT_DATA
 * - Workspace member service check MANAGE_MEMBERS
 * - Dashboard service check READ_WORKSPACE
 */
public enum WorkspacePermission {

    /**
     * Được xem workspace, danh sách store, dashboard, report cơ bản.
     */
    READ_WORKSPACE,

    /**
     * Được upload file Excel/CSV và chạy ETL import.
     */
    IMPORT_DATA,

    /**
     * Được xác nhận/chỉnh mapping cột trước khi import.
     */
    CONFIRM_MAPPING,

    /**
     * Được quản lý store/shop trong workspace.
     */
    MANAGE_STORES,

    /**
     * Được quản lý thành viên workspace: mời, đổi role, xóa member.
     */
    MANAGE_MEMBERS,

    /**
     * Được quản lý dữ liệu tài chính: giá vốn, chi phí vận hành, profit config.
     */
    MANAGE_FINANCIAL_DATA,

    /**
     * Được xem dữ liệu tài chính: profit, cost, expense.
     */
    READ_FINANCIAL_DATA,

    /**
     * Được tạo/sửa/xóa mapping template.
     */
    MANAGE_MAPPING_TEMPLATES,

    /**
     * Được tạo/sửa/xem insight AI.
     */
    MANAGE_INSIGHTS,

    /**
     * Được xem audit log/import history.
     */
    READ_AUDIT_LOGS,

    /**
     * Được quản lý billing/subscription của workspace.
     */
    MANAGE_BILLING,

    /**
     * Chỉ owner mới nên có quyền này.
     */
    DELETE_WORKSPACE
}