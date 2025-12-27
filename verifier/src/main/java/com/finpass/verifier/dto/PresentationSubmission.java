package com.finpass.verifier.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * OpenID4VP Presentation Submission
 * Contains the presented credentials and submission metadata
 */
public class PresentationSubmission {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("definition_id")
    private String definitionId;
    
    @JsonProperty("descriptor_map")
    private List<DescriptorMap> descriptorMap;
    
    @JsonProperty("format")
    private PresentationFormat format;
    
    // Constructors
    public PresentationSubmission() {}
    
    public PresentationSubmission(String id, String definitionId) {
        this.id = id;
        this.definitionId = definitionId;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getDefinitionId() { return definitionId; }
    public void setDefinitionId(String definitionId) { this.definitionId = definitionId; }
    
    public List<DescriptorMap> getDescriptorMap() { return descriptorMap; }
    public void setDescriptorMap(List<DescriptorMap> descriptorMap) { this.descriptorMap = descriptorMap; }
    
    public PresentationFormat getFormat() { return format; }
    public void setFormat(PresentationFormat format) { this.format = format; }
    
    /**
     * Descriptor map for submission
     */
    public static class DescriptorMap {
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("format")
        private String format;
        
        @JsonProperty("path")
        private String path;
        
        @JsonProperty("path_nested")
        private PathNested pathNested;
        
        public DescriptorMap() {}
        
        public DescriptorMap(String id, String format, String path) {
            this.id = id;
            this.format = format;
            this.path = path;
        }
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public PathNested getPathNested() { return pathNested; }
        public void setPathNested(PathNested pathNested) { this.pathNested = pathNested; }
    }
    
    /**
     * Path nested structure
     */
    public static class PathNested {
        @JsonProperty("path")
        private String path;
        
        @JsonProperty("format")
        private String format;
        
        public PathNested() {}
        
        public PathNested(String path, String format) {
            this.path = path;
            this.format = format;
        }
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
    }
    
    /**
     * Presentation format for submission
     */
    public static class PresentationFormat {
        @JsonProperty("jwt")
        private String jwt;
        
        @JsonProperty("jwt_vc")
        private String jwtVc;
        
        @JsonProperty("jwt_vp")
        private String jwtVp;
        
        public PresentationFormat() {}
        
        public String getJwt() { return jwt; }
        public void setJwt(String jwt) { this.jwt = jwt; }
        
        public String getJwtVc() { return jwtVc; }
        public void setJwtVc(String jwtVc) { this.jwtVc = jwtVc; }
        
        public String getJwtVp() { return jwtVp; }
        public void setJwtVp(String jwtVp) { this.jwtVp = jwtVp; }
    }
}
