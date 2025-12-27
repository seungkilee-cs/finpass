package com.finpass.verifier.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * OpenID4VP Presentation Definition
 * Defines the requirements for credential presentation
 */
public class PresentationDefinition {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("purpose")
    private String purpose;
    
    @JsonProperty("format")
    private PresentationFormat format;
    
    @JsonProperty("input_descriptors")
    private List<InputDescriptor> inputDescriptors;
    
    @JsonProperty("submission_requirements")
    private List<SubmissionRequirement> submissionRequirements;
    
    // Constructors
    public PresentationDefinition() {}
    
    public PresentationDefinition(String id, String name, String purpose) {
        this.id = id;
        this.name = name;
        this.purpose = purpose;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    
    public PresentationFormat getFormat() { return format; }
    public void setFormat(PresentationFormat format) { this.format = format; }
    
    public List<InputDescriptor> getInputDescriptors() { return inputDescriptors; }
    public void setInputDescriptors(List<InputDescriptor> inputDescriptors) { this.inputDescriptors = inputDescriptors; }
    
    public List<SubmissionRequirement> getSubmissionRequirements() { return submissionRequirements; }
    public void setSubmissionRequirements(List<SubmissionRequirement> submissionRequirements) { this.submissionRequirements = submissionRequirements; }
    
    /**
     * Presentation format requirements
     */
    public static class PresentationFormat {
        @JsonProperty("jwt")
        private JwtFormat jwt;
        
        @JsonProperty("jwt_vc")
        private JwtFormat jwtVc;
        
        @JsonProperty("jwt_vp")
        private JwtFormat jwtVp;
        
        @JsonProperty("ldp")
        private LdpFormat ldp;
        
        @JsonProperty("ldp_vc")
        private LdpFormat ldpVc;
        
        @JsonProperty("ldp_vp")
        private LdpFormat ldpVp;
        
        public PresentationFormat() {}
        
        public JwtFormat getJwt() { return jwt; }
        public void setJwt(JwtFormat jwt) { this.jwt = jwt; }
        
        public JwtFormat getJwtVc() { return jwtVc; }
        public void setJwtVc(JwtFormat jwtVc) { this.jwtVc = jwtVc; }
        
        public JwtFormat getJwtVp() { return jwtVp; }
        public void setJwtVp(JwtFormat jwtVp) { this.jwtVp = jwtVp; }
        
        public LdpFormat getLdp() { return ldp; }
        public void setLdp(LdpFormat ldp) { this.ldp = ldp; }
        
        public LdpFormat getLdpVc() { return ldpVc; }
        public void setLdpVc(LdpFormat ldpVc) { this.ldpVc = ldpVc; }
        
        public LdpFormat getLdpVp() { return ldpVp; }
        public void setLdpVp(LdpFormat ldpVp) { this.ldpVp = ldpVp; }
    }
    
    /**
     * JWT format specification
     */
    public static class JwtFormat {
        @JsonProperty("alg")
        private List<String> alg;
        
        public JwtFormat() {}
        
        public JwtFormat(List<String> alg) {
            this.alg = alg;
        }
        
        public List<String> getAlg() { return alg; }
        public void setAlg(List<String> alg) { this.alg = alg; }
    }
    
    /**
     * Linked Data Proof format specification
     */
    public static class LdpFormat {
        @JsonProperty("proof_type")
        private List<String> proofType;
        
        public LdpFormat() {}
        
        public LdpFormat(List<String> proofType) {
            this.proofType = proofType;
        }
        
        public List<String> getProofType() { return proofType; }
        public void setProofType(List<String> proofType) { this.proofType = proofType; }
    }
    
    /**
     * Input descriptor for credential requirements
     */
    public static class InputDescriptor {
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("purpose")
        private String purpose;
        
        @JsonProperty("format")
        private PresentationFormat format;
        
        @JsonProperty("constraints")
        private Constraints constraints;
        
        public InputDescriptor() {}
        
        public InputDescriptor(String id, String name, String purpose) {
            this.id = id;
            this.name = name;
            this.purpose = purpose;
        }
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getPurpose() { return purpose; }
        public void setPurpose(String purpose) { this.purpose = purpose; }
        
        public PresentationFormat getFormat() { return format; }
        public void setFormat(PresentationFormat format) { this.format = format; }
        
        public Constraints getConstraints() { return constraints; }
        public void setConstraints(Constraints constraints) { this.constraints = constraints; }
    }
    
    /**
     * Constraints for input descriptor
     */
    public static class Constraints {
        @JsonProperty("fields")
        private List<Field> fields;
        
        @JsonProperty("limit_disclosure")
        private String limitDisclosure;
        
        public Constraints() {}
        
        public List<Field> getFields() { return fields; }
        public void setFields(List<Field> fields) { this.fields = fields; }
        
        public String getLimitDisclosure() { return limitDisclosure; }
        public void setLimitDisclosure(String limitDisclosure) { this.limitDisclosure = limitDisclosure; }
    }
    
    /**
     * Field specification for constraints
     */
    public static class Field {
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("path")
        private List<String> path;
        
        @JsonProperty("purpose")
        private String purpose;
        
        @JsonProperty("filter")
        private Map<String, Object> filter;
        
        public Field() {}
        
        public Field(String id, List<String> path, String purpose) {
            this.id = id;
            this.path = path;
            this.purpose = purpose;
        }
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public List<String> getPath() { return path; }
        public void setPath(List<String> path) { this.path = path; }
        
        public String getPurpose() { return purpose; }
        public void setPurpose(String purpose) { this.purpose = purpose; }
        
        public Map<String, Object> getFilter() { return filter; }
        public void setFilter(Map<String, Object> filter) { this.filter = filter; }
    }
    
    /**
     * Submission requirements
     */
    public static class SubmissionRequirement {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("rule")
        private String rule;
        
        @JsonProperty("count")
        private Integer count;
        
        @JsonProperty("min")
        private Integer min;
        
        @JsonProperty("max")
        private Integer max;
        
        @JsonProperty("from")
        private String from;
        
        public SubmissionRequirement() {}
        
        public SubmissionRequirement(String name, String rule, String from) {
            this.name = name;
            this.rule = rule;
            this.from = from;
        }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getRule() { return rule; }
        public void setRule(String rule) { this.rule = rule; }
        
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
        
        public Integer getMin() { return min; }
        public void setMin(Integer min) { this.min = min; }
        
        public Integer getMax() { return max; }
        public void setMax(Integer max) { this.max = max; }
        
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
    }
}
