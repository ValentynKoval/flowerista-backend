package ua.flowerista.shop.services;


import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.orders.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ua.flowerista.shop.models.CompletedOrder;
import ua.flowerista.shop.models.PaymentOrder;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaypalService {
    public static final String SUCCESS_URL = "/api/payment/capture";
    public static final String CANCEL_URL = "/checkout";
    @Value("${server.url}")
    private String serverUrl;
    @Value("${front.url}")
    private String frontUrl;
    private final PayPalHttpClient payPalHttpClient;
    private final OrderService orderService;

    public PaymentOrder createPayment(BigDecimal fee, String currencyCode, Integer orderId) {
        //TODO: add validation for sum and currency and order total sum
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.checkoutPaymentIntent("CAPTURE");
        AmountWithBreakdown amountBreakdown = new AmountWithBreakdown().currencyCode(currencyCode).value(fee.toString());
        PurchaseUnitRequest purchaseUnitRequest = new PurchaseUnitRequest().amountWithBreakdown(amountBreakdown);
        orderRequest.purchaseUnits(List.of(purchaseUnitRequest));
        ApplicationContext applicationContext = new ApplicationContext()
                .returnUrl(serverUrl + SUCCESS_URL)
                .cancelUrl(frontUrl + CANCEL_URL);
        orderRequest.applicationContext(applicationContext);
        OrdersCreateRequest ordersCreateRequest = new OrdersCreateRequest().requestBody(orderRequest);

        try {
            HttpResponse<Order> orderHttpResponse = payPalHttpClient.execute(ordersCreateRequest);
            Order order = orderHttpResponse.result();

            String redirectUrl = order.links().stream()
                    .filter(link -> "approve".equals(link.rel()))
                    .findFirst()
                    .orElseThrow(NoSuchElementException::new)
                    .href();
            orderService.updatePayId(orderId, order.id());
            orderService.updateStatus(orderId, "PENDING");
            return new PaymentOrder("success", order.id(), redirectUrl);
        } catch (IOException e) {
            log.error(e.getMessage());
            return new PaymentOrder("Error");
        }
    }


    public CompletedOrder completePayment(String token) {
        OrdersCaptureRequest ordersCaptureRequest = new OrdersCaptureRequest(token);
        try {
            HttpResponse<Order> httpResponse = payPalHttpClient.execute(ordersCaptureRequest);
            if (httpResponse.result().status() != null) {
                orderService.updateStatusByPayId(token, "IN_PROCESS");
                return new CompletedOrder("success", token);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return new CompletedOrder("error");
    }

}
