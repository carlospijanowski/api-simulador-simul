package br.com.bancotoyota.services.simulador.beans.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.co.jemos.podam.api.AttributeMetadata;
import uk.co.jemos.podam.api.DataProviderStrategy;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;
import uk.co.jemos.podam.common.PodamExclude;
import uk.co.jemos.podam.typeManufacturers.AbstractTypeManufacturer;
import uk.co.jemos.podam.typeManufacturers.DoubleTypeManufacturerImpl;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@JsonTest
public class TestesParaEqualsEHashcodeGeradosPeloLombok {

    @Autowired
    private ObjectMapper mapper;

    private PodamFactory factory = new PodamFactoryImpl();

    @Before
    public void setup() {
        factory.getStrategy().addOrReplaceTypeManufacturer(BigDecimal.class, new CustomBigDecimalManufacturer());
    }

    /**
     * Valida que os métodos equals e hashCode estão funcionando conforme esperado pois o código depende disso.
     * Essa classe serve apenas para que o Sonar não reclame da cobertura quando temos classes com Equals e Hashcode
     * geradas pelo Lombok.
     */
    @Ignore
    @Test
    public void testaPrazoResidualResponse() throws Exception {
        equals(PrazoResidualResponse.class);
    }
    @Ignore
    @Test
    public void testaPrazoDesejadaResponse() throws Exception {
        equals(PrazoDesejadaResponse.class);
    }

    @Test
    public void testaSubsidio() throws Exception {
        equals(ValorSubsidio.class);
    }

    public <T> void equals(Class<T> cls) throws Exception {

        T objeto1 = factory.manufacturePojo(cls);
        T objeto2 = factory.manufacturePojo(cls);
        assertNotSame("objetos devem ser diferentes", objeto1, objeto2);
        assertNotEquals("objetos devem ser diferentes", objeto1, objeto2);
        assertEquals("objeto deve ser igual", objeto1, objeto1);
        assertNotEquals("objeto deve ser igual", objeto1, "string");
        String str = mapper.writeValueAsString(objeto1);
        T objeto3 = mapper.readValue(str, cls);
        assertEquals(objeto1, objeto3);
        testaComUmDosCamposNulos(objeto1, objeto3);
        System.out.println(objeto1);
        assertEquals(objeto1.hashCode(), objeto3.hashCode());
        assertNotEquals(objeto1.hashCode(), objeto2.hashCode());
    }

    /**
     *  Este método é usado para aumentar a cobertura do método equals gerado pelo Lombok para não atrapalhar o nível
     *  de cobertura do código no geral.
     */
    private <T> void testaComUmDosCamposNulos(T objetoA, T objetoB) throws Exception {
        PropertyDescriptor[] propertyDescriptors = BeanUtils.getPropertyDescriptors(objetoA.getClass());
        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            if (propertyDescriptor.getName().equals("class")) {
                continue;
            }
            Field field = getDeclaredField(objetoA.getClass(), propertyDescriptor.getName());
            if (field.getAnnotation(PodamExclude.class) != null) {
                continue;
            }

            Object valorOriginalA = propertyDescriptor.getReadMethod().invoke(objetoA);
            propertyDescriptor.getWriteMethod().invoke(objetoA, new Object[] {null});
            assertNotEquals(objetoA, objetoB);
            Object valorOriginalB = propertyDescriptor.getReadMethod().invoke(objetoB);
            propertyDescriptor.getWriteMethod().invoke(objetoB, new Object[] {null});
            assertEquals(objetoA, objetoB);
            assertEquals(objetoA.hashCode(), objetoB.hashCode());
            propertyDescriptor.getWriteMethod().invoke(objetoA, valorOriginalA);
            assertNotEquals(objetoA, objetoB);
            propertyDescriptor.getWriteMethod().invoke(objetoB, valorOriginalB);
            assertEquals(objetoA, objetoB);
            assertEquals(objetoA.hashCode(), objetoB.hashCode());
        }
    }

    private <T> Field getDeclaredField(Class cls, String name) throws NoSuchFieldException {
        try {
            return cls.getDeclaredField(name);
        } catch (NoSuchFieldException ex) {
            if (cls == Object.class) {
                throw ex;
            } else {
                return getDeclaredField(cls.getSuperclass(), name);
            }
        }
    }


    public class CustomBigDecimalManufacturer extends AbstractTypeManufacturer<BigDecimal> {

        private DoubleTypeManufacturerImpl delegate = new DoubleTypeManufacturerImpl();

        @Override
        public BigDecimal getType(DataProviderStrategy strategy,
                              AttributeMetadata attributeMetadata,
                              Map<String, Type> genericTypesArgumentsMap) {

            double d = delegate.getType(strategy, attributeMetadata, genericTypesArgumentsMap);

            return new BigDecimal(d);
        }
    }
}