package ir.darkdeveloper.anbarinoo.model.Financial;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import ir.darkdeveloper.anbarinoo.model.ProductModel;
import ir.darkdeveloper.anbarinoo.model.deserializers.BuyDeserializer;
import ir.darkdeveloper.anbarinoo.model.serializers.BuySerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "buys")
@JsonDeserialize(using = BuyDeserializer.class)
@JsonSerialize(using = BuySerialize.class)
@AllArgsConstructor
@NoArgsConstructor
public class BuyModel {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal count;

    // precision means the whole numbers contains in decimal or integer
    // scale means the count of numbers after . or point
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer tax = 9;

    @ManyToOne
    @JoinColumn(name = "product_id", referencedColumnName = "id")
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    private ProductModel product;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void update(BuyModel other) {
        count = other.count != null || count == null ? other.count : count;
        price = other.price != null || price == null ? other.price : price;
        tax = other.tax != null || tax == null ? other.tax : tax;
    }
}
