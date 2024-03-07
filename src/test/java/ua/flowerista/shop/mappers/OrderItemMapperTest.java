package ua.flowerista.shop.mappers;

import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.flowerista.shop.dto.OrderItemDto;
import ua.flowerista.shop.models.*;
import ua.flowerista.shop.repo.BouqueteRepository;
import ua.flowerista.shop.repo.BouqueteSizeRepository;
import ua.flowerista.shop.repo.ColorRepository;

import java.math.BigInteger;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class OrderItemMapperTest {

    @Mock
    ColorRepository colorRepository;
    @Mock
    BouqueteSizeRepository bouqueteSizeRepository;
    @Mock
    BouqueteRepository bouqueteRepository;
    @InjectMocks
    OrderItemMapper orderItemMapper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }


    //@Test
    public void toEntityTestWithoutSalePrice() {
        //given
        mockRepositories();

        OrderItemDto dto = new OrderItemDto();
        dto.setProductId(1);
        dto.setName("Bouquete");
        dto.setQuantity(1);
        dto.setSizeId(1);
        dto.setPrice(BigInteger.valueOf(100));
        //when
        OrderItem orderItem = orderItemMapper.toEntity(dto);
        //then
        assertEquals(1, orderItem.getBouquete().getId());
        assertEquals("Bouquete", orderItem.getName());
        assertEquals(1, orderItem.getQuantity());
        assertEquals(1, orderItem.getSize().getId());
        assertEquals(100, orderItem.getPrice().intValue());
    }
    //@Test
    public void toEntityTestWithSalePrice() {
        //given
        mockRepositories();

        BouqueteSize size = new BouqueteSize();
        size.setId(1);
        size.setDefaultPrice(BigInteger.valueOf(100));
        size.setDiscountPrice(BigInteger.valueOf(90));
        size.setDiscount(BigInteger.valueOf(10));
        size.setIsSale(true);
        Mockito.when(bouqueteSizeRepository.findById(1)).thenReturn(java.util.Optional.of(size));

        OrderItemDto dto = new OrderItemDto();
        dto.setProductId(1);
        dto.setName("Bouquete");
        dto.setQuantity(1);
        dto.setSizeId(1);
        dto.setPrice(BigInteger.valueOf(90));
        //when
        OrderItem orderItem = orderItemMapper.toEntity(dto);
        //then
        assertEquals(90, orderItem.getPrice().intValue());
    }

    @Test
    public void toDto() {
        //given
        OrderItem orderItem = new OrderItem();
        orderItem.setId(1);
        Bouquete bouquete = new Bouquete();
        bouquete.setId(1);
        orderItem.setBouquete(bouquete);
        orderItem.setName("Bouquete");
        orderItem.setQuantity(1);
        Color color = new Color();
        color.setId(1);
        BouqueteSize size = new BouqueteSize();
        size.setId(1);
        orderItem.setSize(size);
        orderItem.setPrice(BigInteger.valueOf(100));
        //when
        OrderItemDto dto = orderItemMapper.toDto(orderItem);
        //then
        assertEquals("Bouquete", dto.getName());
        assertEquals(1, dto.getQuantity());
        assertEquals(1, dto.getSizeId());
        assertEquals(100, dto.getPrice().intValue());
    }

    private void mockRepositories() {
        BouqueteSize size = new BouqueteSize();
        size.setId(1);
        size.setDefaultPrice(BigInteger.valueOf(100));
        size.setDiscountPrice(BigInteger.valueOf(90));
        size.setDiscount(BigInteger.valueOf(10));
        size.setIsSale(false);
        Mockito.when(bouqueteSizeRepository.findById(1)).thenReturn(java.util.Optional.of(size));

        Bouquete bouquete = new Bouquete();
        bouquete.setId(1);
        bouquete.setName("Bouquete");
        bouquete.setQuantity(10);
        bouquete.setSoldQuantity(0);
        Flower flower = new Flower();
        flower.setId(1);
        flower.setName("Rose");
        bouquete.setFlowers(Set.of(flower));
        bouquete.setSizes(Set.of(size));
        Mockito.when(bouqueteRepository.findById(1)).thenReturn(bouquete);
    }

}
