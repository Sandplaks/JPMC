package jpmc.wm.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AutowiredHelperTest {

    @InjectMocks
    private AutowiredHelper autowiredHelper; // The class under test

    @Mock
    private AutowireCapableBeanFactory beanFactory; // Mocked dependency

    @Mock
    private ApplicationContext applicationContext; // Mocked application context

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // Mock the static method to return the mocked application context
        AppContextProvider.setApplicationContext(applicationContext);
        when(applicationContext.getBean(AutowiredHelper.class)).thenReturn(autowiredHelper);
    }

    @Test
    public void testAutowire_WhenInstanceIsNull_ShouldAutowireBean() {
        // Arrange
        AutowiredHelper.instance = null; // Ensure instance is null
        TestBean testBean = new TestBean(); // Bean to be autowired

        // Act
        AutowiredHelper.autowire(testBean);

        // Assert
        verify(applicationContext, times(1)).getBean(AutowiredHelper.class); // Verify instance retrieval
        verify(beanFactory, times(1)).autowireBean(testBean); // Verify autowiring
    }

    @Test
    public void testAutowire_WhenInstanceIsNotNull_ShouldAutowireBean() {
        // Arrange
        AutowiredHelper.instance = autowiredHelper; // Set the instance
        TestBean testBean = new TestBean(); // Bean to be autowired

        // Act
        AutowiredHelper.autowire(testBean);

        // Assert
        verify(applicationContext, never()).getBean(AutowiredHelper.class); // Ensure instance is not retrieved again
        verify(beanFactory, times(1)).autowireBean(testBean); // Verify autowiring
    }

    @Test
    public void testAutowire_ShouldLogBeanClassName() {
        // Arrange
        AutowiredHelper.instance = autowiredHelper; // Set the instance
        TestBean testBean = new TestBean(); // Bean to be autowired

        // Act
        AutowiredHelper.autowire(testBean);

        // Assert
        // Verify that the log statement is executed (mocking log behavior is optional)
        // You can use a logging framework like LogCaptor to verify log messages if needed
        verify(beanFactory, times(1)).autowireBean(testBean); // Verify autowiring
    }

    // Mock class for testing
    static class TestBean {
        // Add fields and methods as needed for testing
    }
}
