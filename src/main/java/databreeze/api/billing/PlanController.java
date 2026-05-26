//package databreeze.api.billing;
//
//import databreeze.dto.billing.PlanResponse;
//import databreeze.service.billing.SubscriptionService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import java.util.List;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping("/api/v1/plans")
//@Tag(name = "Subscription Plans", description = "Danh sách gói subscription và quota token AI.")
//public class PlanController {
//    @Autowired
//    private SubscriptionService subscriptionService;
//
//    @GetMapping
//    @Operation(summary = "Xem các gói subscription")
//    public List<PlanResponse> listPlans() {
//        return subscriptionService.listPlans();
//    }
//}
