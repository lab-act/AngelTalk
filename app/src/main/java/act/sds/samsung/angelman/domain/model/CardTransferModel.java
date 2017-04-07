package act.sds.samsung.angelman.domain.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;


@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CardTransferModel {
    public String cardType;
    public String name;
    public String contentPath;
    public String thumbnailPath;
}

