package gov.samhsa.c2s.pep.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.samhsa.c2s.pep.infrastructure.dto.PatientIdDto;
import gov.samhsa.c2s.pep.infrastructure.dto.SubjectPurposeOfUse;
import gov.samhsa.c2s.pep.infrastructure.dto.XacmlRequestDto;
import gov.samhsa.c2s.pep.service.PolicyEnforcementPointService;
import gov.samhsa.c2s.pep.service.dto.AccessResponseDto;
import gov.samhsa.c2s.pep.service.dto.AccessResponseWithDocumentDto;
import gov.samhsa.c2s.pep.service.exception.NoDocumentFoundException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import static gov.samhsa.c2s.common.unit.matcher.ArgumentMatchers.matching;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class PolicyEnforcementPointRestControllerTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private MockMvc mvc;
    private ObjectMapper objectMapper;

    @Mock
    private PolicyEnforcementPointService policyEnforcementPointService;

    @InjectMocks
    private PolicyEnforcementPointRestController sut;

    @Before
    public void setup() {
        mvc = MockMvcBuilders.standaloneSetup(this.sut).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    public void access() throws Exception {
        // Arrange
        final String recipientNpi = "recipientNpi";
        final String intermediaryNpi = "intermediaryNpi";
        final SubjectPurposeOfUse purposeOfUse = SubjectPurposeOfUse.HEALTHCARE_TREATMENT;
        final String extension = "extension";
        final String root = "root";
        final PatientIdDto patientId = PatientIdDto.builder().extension(extension).root(root).build();
        final XacmlRequestDto xacmlRequest = XacmlRequestDto.builder().intermediaryNpi(intermediaryNpi).recipientNpi(recipientNpi).patientId(patientId).purposeOfUse(purposeOfUse).build();
        final String document = "document";
        final Charset documentEncoding = StandardCharsets.UTF_8;
        final byte[] documentBytes = document.getBytes(StandardCharsets.UTF_8);
        final String documentEncodingString = documentEncoding.name();
        final AccessRequestDtoForTest request = AccessRequestDtoForTest.builder()
                .xacmlRequest(xacmlRequest)
                .document(documentBytes)
                .documentEncoding(documentEncodingString)
                .build();
        final String segmentedDocument = "segmentedDocument";
        final byte[] segmentedDocumentBytes = segmentedDocument.getBytes(documentEncoding);
        final String segmentedDocumentBytesEncodedString = Base64.getEncoder().encodeToString(segmentedDocumentBytes);
        final AccessResponseDto response = AccessResponseWithDocumentDto.builder()
                .segmentedDocument(segmentedDocumentBytes)
                .segmentedDocumentEncoding(documentEncodingString)
                .build();
        when(policyEnforcementPointService.accessDocument(argThat(matching(
                req -> req.getXacmlRequest().equals(xacmlRequest) &&
                        document.equals(new String(req.getDocument().get(), documentEncoding)) &&
                        documentEncodingString.equals(req.getDocumentEncoding().get())
        )), eq(Optional.empty()))).thenReturn(response);

        // Act and Assert
        mvc.perform(post("/access")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.segmentedDocument", is(segmentedDocumentBytesEncodedString)))
                .andExpect(jsonPath("$.segmentedDocumentEncoding", is(documentEncodingString)));
        verify(policyEnforcementPointService, times(1)).accessDocument(argThat(matching(
                req -> req.getXacmlRequest().equals(xacmlRequest) &&
                        document.equals(new String(req.getDocument().get(), documentEncoding)) &&
                        documentEncodingString.equals(req.getDocumentEncoding().get())
        )), eq(Optional.empty()));
    }

    @Test
    public void access_Throws_DocumentNotFoundException() throws Exception {
        // Arrange
        final String recipientNpi = "recipientNpi";
        final String intermediaryNpi = "intermediaryNpi";
        final SubjectPurposeOfUse purposeOfUse = SubjectPurposeOfUse.HEALTHCARE_TREATMENT;
        final String extension = "extension";
        final String root = "root";
        final PatientIdDto patientId = PatientIdDto.builder().extension(extension).root(root).build();
        final XacmlRequestDto xacmlRequest = XacmlRequestDto.builder().intermediaryNpi(intermediaryNpi).recipientNpi(recipientNpi).patientId(patientId).purposeOfUse(purposeOfUse).build();
        final String document = "document";
        final Charset documentEncoding = StandardCharsets.UTF_8;
        final byte[] documentBytes = document.getBytes(StandardCharsets.UTF_8);
        final String documentEncodingString = documentEncoding.name();
        final AccessRequestDtoForTest request = AccessRequestDtoForTest.builder()
                .xacmlRequest(xacmlRequest)
                .document(documentBytes)
                .documentEncoding(documentEncodingString)
                .build();
        when(policyEnforcementPointService.accessDocument(argThat(matching(
                req -> req.getXacmlRequest().equals(xacmlRequest) &&
                        document.equals(new String(req.getDocument().get(), documentEncoding)) &&
                        documentEncodingString.equals(req.getDocumentEncoding().get())
        )), eq(Optional.empty()))).thenThrow(NoDocumentFoundException.class);

        // Act and Assert
        mvc.perform(post("/access")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isNotFound());
        verify(policyEnforcementPointService, times(1)).accessDocument(argThat(matching(
                req -> req.getXacmlRequest().equals(xacmlRequest) &&
                        document.equals(new String(req.getDocument().get(), documentEncoding)) &&
                        documentEncodingString.equals(req.getDocumentEncoding().get())
        )), eq(Optional.empty()));
    }
}