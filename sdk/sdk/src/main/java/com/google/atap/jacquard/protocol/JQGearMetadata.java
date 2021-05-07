/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.atap.jacquard.protocol;

/**
 * Generated java proto file having details of vendors and products along-with models to hold data
 * from metadata.json config file from "raw" directory.
 */
public final class JQGearMetadata {
  private JQGearMetadata() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }
  public interface GearMetadataOrBuilder extends
      // @@protoc_insertion_point(interface_extends:GearMetadata)
      com.google.protobuf.MessageLiteOrBuilder {
  }
  /**
   * Protobuf type {@code GearMetadata}
   */
  public  static final class GearMetadata extends
      com.google.protobuf.GeneratedMessageLite<
          GearMetadata, GearMetadata.Builder> implements
      // @@protoc_insertion_point(message_implements:GearMetadata)
      GearMetadataOrBuilder {
    private GearMetadata() {
    }
    /**
     * Protobuf enum {@code GearMetadata.Capability}
     */
    public enum Capability
        implements com.google.protobuf.Internal.EnumLite {
      /**
       * <code>LED = 0;</code>
       */
      LED(0),
      /**
       * <code>GESTURE = 1;</code>
       */
      GESTURE(1),
      /**
       * <code>TOUCH_DATA_STREAM = 2;</code>
       */
      TOUCH_DATA_STREAM(2),
      /**
       * <code>HAPTIC = 3;</code>
       */
      HAPTIC(3),
      UNRECOGNIZED(-1),
      ;

      /**
       * <code>LED = 0;</code>
       */
      public static final int LED_VALUE = 0;
      /**
       * <code>GESTURE = 1;</code>
       */
      public static final int GESTURE_VALUE = 1;
      /**
       * <code>TOUCH_DATA_STREAM = 2;</code>
       */
      public static final int TOUCH_DATA_STREAM_VALUE = 2;
      /**
       * <code>HAPTIC = 3;</code>
       */
      public static final int HAPTIC_VALUE = 3;


      public final int getNumber() {
        return value;
      }

      /**
       * @deprecated Use {@link #forNumber(int)} instead.
       */
      @Deprecated
      public static Capability valueOf(int value) {
        return forNumber(value);
      }

      public static Capability forNumber(int value) {
        switch (value) {
          case 0: return LED;
          case 1: return GESTURE;
          case 2: return TOUCH_DATA_STREAM;
          case 3: return HAPTIC;
          default: return null;
        }
      }

      public static com.google.protobuf.Internal.EnumLiteMap<Capability>
          internalGetValueMap() {
        return internalValueMap;
      }
      private static final com.google.protobuf.Internal.EnumLiteMap<
          Capability> internalValueMap =
            new com.google.protobuf.Internal.EnumLiteMap<Capability>() {
              public Capability findValueByNumber(int number) {
                return Capability.forNumber(number);
              }
            };

      private final int value;

      private Capability(int value) {
        this.value = value;
      }

      // @@protoc_insertion_point(enum_scope:GearMetadata.Capability)
    }

    public interface GearDataOrBuilder extends
        // @@protoc_insertion_point(interface_extends:GearMetadata.GearData)
        com.google.protobuf.MessageLiteOrBuilder {

      /**
       * <code>repeated .GearMetadata.GearData.Vendor vendors = 1;</code>
       */
      java.util.List<GearData.Vendor>
          getVendorsList();
      /**
       * <code>repeated .GearMetadata.GearData.Vendor vendors = 1;</code>
       */
      GearData.Vendor getVendors(int index);
      /**
       * <code>repeated .GearMetadata.GearData.Vendor vendors = 1;</code>
       */
      int getVendorsCount();
    }
    /**
     * Protobuf type {@code GearMetadata.GearData}
     */
    public  static final class GearData extends
        com.google.protobuf.GeneratedMessageLite<
            GearData, GearData.Builder> implements
        // @@protoc_insertion_point(message_implements:GearMetadata.GearData)
        GearDataOrBuilder {
      private GearData() {
        vendors_ = emptyProtobufList();
      }
      public interface ProductOrBuilder extends
          // @@protoc_insertion_point(interface_extends:GearMetadata.GearData.Product)
          com.google.protobuf.MessageLiteOrBuilder {

        /**
         * <code>optional string id = 1;</code>
         */
        String getId();
        /**
         * <code>optional string id = 1;</code>
         */
        com.google.protobuf.ByteString
            getIdBytes();

        /**
         * <code>optional string name = 2;</code>
         */
        String getName();
        /**
         * <code>optional string name = 2;</code>
         */
        com.google.protobuf.ByteString
            getNameBytes();

        /**
         * <code>optional string image = 3;</code>
         */
        String getImage();
        /**
         * <code>optional string image = 3;</code>
         */
        com.google.protobuf.ByteString
            getImageBytes();

        /**
         * <code>repeated .GearMetadata.Capability capabilities = 4;</code>
         */
        java.util.List<Capability> getCapabilitiesList();
        /**
         * <code>repeated .GearMetadata.Capability capabilities = 4;</code>
         */
        int getCapabilitiesCount();
        /**
         * <code>repeated .GearMetadata.Capability capabilities = 4;</code>
         */
        Capability getCapabilities(int index);
        /**
         * <code>repeated .GearMetadata.Capability capabilities = 4;</code>
         */
        java.util.List<Integer>
        getCapabilitiesValueList();
        /**
         * <code>repeated .GearMetadata.Capability capabilities = 4;</code>
         */
        int getCapabilitiesValue(int index);
      }
      /**
       * Protobuf type {@code GearMetadata.GearData.Product}
       */
      public  static final class Product extends
          com.google.protobuf.GeneratedMessageLite<
              Product, Product.Builder> implements
          // @@protoc_insertion_point(message_implements:GearMetadata.GearData.Product)
          ProductOrBuilder {
        private Product() {
          id_ = "";
          name_ = "";
          image_ = "";
          capabilities_ = emptyIntList();
        }
        private int bitField0_;
        public static final int ID_FIELD_NUMBER = 1;
        private String id_;
        /**
         * <code>optional string id = 1;</code>
         */
        public String getId() {
          return id_;
        }
        /**
         * <code>optional string id = 1;</code>
         */
        public com.google.protobuf.ByteString
            getIdBytes() {
          return com.google.protobuf.ByteString.copyFromUtf8(id_);
        }
        /**
         * <code>optional string id = 1;</code>
         */
        private void setId(
            String value) {
          if (value == null) {
    throw new NullPointerException();
  }
  
          id_ = value;
        }
        /**
         * <code>optional string id = 1;</code>
         */
        private void clearId() {
          
          id_ = getDefaultInstance().getId();
        }
        /**
         * <code>optional string id = 1;</code>
         */
        private void setIdBytes(
            com.google.protobuf.ByteString value) {
          if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
          
          id_ = value.toStringUtf8();
        }

        public static final int NAME_FIELD_NUMBER = 2;
        private String name_;
        /**
         * <code>optional string name = 2;</code>
         */
        public String getName() {
          return name_;
        }
        /**
         * <code>optional string name = 2;</code>
         */
        public com.google.protobuf.ByteString
            getNameBytes() {
          return com.google.protobuf.ByteString.copyFromUtf8(name_);
        }
        /**
         * <code>optional string name = 2;</code>
         */
        private void setName(
            String value) {
          if (value == null) {
    throw new NullPointerException();
  }
  
          name_ = value;
        }
        /**
         * <code>optional string name = 2;</code>
         */
        private void clearName() {
          
          name_ = getDefaultInstance().getName();
        }
        /**
         * <code>optional string name = 2;</code>
         */
        private void setNameBytes(
            com.google.protobuf.ByteString value) {
          if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
          
          name_ = value.toStringUtf8();
        }

        public static final int IMAGE_FIELD_NUMBER = 3;
        private String image_;
        /**
         * <code>optional string image = 3;</code>
         */
        public String getImage() {
          return image_;
        }
        /**
         * <code>optional string image = 3;</code>
         */
        public com.google.protobuf.ByteString
            getImageBytes() {
          return com.google.protobuf.ByteString.copyFromUtf8(image_);
        }
        /**
         * <code>optional string image = 3;</code>
         */
        private void setImage(
            String value) {
          if (value == null) {
    throw new NullPointerException();
  }
  
          image_ = value;
        }
        /**
         * <code>optional string image = 3;</code>
         */
        private void clearImage() {
          
          image_ = getDefaultInstance().getImage();
        }
        /**
         * <code>optional string image = 3;</code>
         */
        private void setImageBytes(
            com.google.protobuf.ByteString value) {
          if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
          
          image_ = value.toStringUtf8();
        }

        public static final int CAPABILITIES_FIELD_NUMBER = 4;
        private com.google.protobuf.Internal.IntList capabilities_;
        private static final com.google.protobuf.Internal.ListAdapter.Converter<
            Integer, Capability> capabilities_converter_ =
                new com.google.protobuf.Internal.ListAdapter.Converter<
                    Integer, Capability>() {
                  public Capability convert(Integer from) {
                    Capability result = Capability.forNumber(from);
                    return result == null ? Capability.UNRECOGNIZED : result;
                  }
                };
        /**
         * <code>repeated .GearMetadata.Capability capabilities = 4;</code>
         */
        public java.util.List<Capability> getCapabilitiesList() {
          return new com.google.protobuf.Internal.ListAdapter<
              Integer, Capability>(capabilities_, capabilities_converter_);
        }
        /**
         * <code>repeated .GearMetadata.Capability capabilities = 4;</code>
         */
        public int getCapabilitiesCount() {
          return capabilities_.size();
        }
        /**
         * <code>repeated .GearMetadata.Capability capabilities = 4;</code>
         */
        public Capability getCapabilities(int index) {
          return capabilities_converter_.convert(capabilities_.getInt(index));
        }
        /**
         * <code>repeated .GearMetadata.Capability capabilities = 4;</code>
         */
        public java.util.List<Integer>
        getCapabilitiesValueList() {
          return capabilities_;
        }
        /**
         * <code>repeated .GearMetadata.Capability capabilities = 4;</code>
         */
        public int getCapabilitiesValue(int index) {
          return capabilities_.getInt(index);
        }
        private void ensureCapabilitiesIsMutable() {
          if (!capabilities_.isModifiable()) {
            capabilities_ =
                com.google.protobuf.GeneratedMessageLite.mutableCopy(capabilities_);
          }
        }
        /**
         * <code>repeated .GearMetadata.Capability capabilities = 4;</code>
         */
        private void setCapabilities(
            int index, Capability value) {
          if (value == null) {
            throw new NullPointerException();
          }
          ensureCapabilitiesIsMutable();
          capabilities_.setInt(index, value.getNumber());
        }
        /**
         * <code>repeated .GearMetadata.Capability capabilities = 4;</code>
         */
        private void addCapabilities(Capability value) {
          if (value == null) {
            throw new NullPointerException();
          }
          ensureCapabilitiesIsMutable();
          capabilities_.addInt(value.getNumber());
        }
        /**
         * <code>repeated .GearMetadata.Capability capabilities = 4;</code>
         */
        private void addAllCapabilities(
            Iterable<? extends Capability> values) {
          ensureCapabilitiesIsMutable();
          for (Capability value : values) {
            capabilities_.addInt(value.getNumber());
          }
        }
        /**
         * <code>repeated .GearMetadata.Capability capabilities = 4;</code>
         */
        private void clearCapabilities() {
          capabilities_ = emptyIntList();
        }
        /**
         * <code>repeated .GearMetadata.Capability capabilities = 4;</code>
         */
        private void setCapabilitiesValue(
            int index, int value) {
          ensureCapabilitiesIsMutable();
          capabilities_.setInt(index, value);
        }
        /**
         * <code>repeated .GearMetadata.Capability capabilities = 4;</code>
         */
        private void addCapabilitiesValue(int value) {
          ensureCapabilitiesIsMutable();
          capabilities_.addInt(value);
        }
        /**
         * <code>repeated .GearMetadata.Capability capabilities = 4;</code>
         */
        private void addAllCapabilitiesValue(
            Iterable<Integer> values) {
          ensureCapabilitiesIsMutable();
          for (int value : values) {
            capabilities_.addInt(value);
          }
        }

        public void writeTo(com.google.protobuf.CodedOutputStream output)
                            throws java.io.IOException {
          getSerializedSize();
          if (!id_.isEmpty()) {
            output.writeString(1, getId());
          }
          if (!name_.isEmpty()) {
            output.writeString(2, getName());
          }
          if (!image_.isEmpty()) {
            output.writeString(3, getImage());
          }
          for (int i = 0; i < capabilities_.size(); i++) {
            output.writeEnum(4, capabilities_.getInt(i));
          }
        }

        public int getSerializedSize() {
          int size = memoizedSerializedSize;
          if (size != -1) return size;

          size = 0;
          if (!id_.isEmpty()) {
            size += com.google.protobuf.CodedOutputStream
              .computeStringSize(1, getId());
          }
          if (!name_.isEmpty()) {
            size += com.google.protobuf.CodedOutputStream
              .computeStringSize(2, getName());
          }
          if (!image_.isEmpty()) {
            size += com.google.protobuf.CodedOutputStream
              .computeStringSize(3, getImage());
          }
          {
            int dataSize = 0;
            for (int i = 0; i < capabilities_.size(); i++) {
              dataSize += com.google.protobuf.CodedOutputStream
                .computeEnumSizeNoTag(capabilities_.getInt(i));
            }
            size += dataSize;
            size += 1 * capabilities_.size();
          }
          memoizedSerializedSize = size;
          return size;
        }

        public static Product parseFrom(
            com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return com.google.protobuf.GeneratedMessageLite.parseFrom(
              DEFAULT_INSTANCE, data);
        }
        public static Product parseFrom(
            com.google.protobuf.ByteString data,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return com.google.protobuf.GeneratedMessageLite.parseFrom(
              DEFAULT_INSTANCE, data, extensionRegistry);
        }
        public static Product parseFrom(byte[] data)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return com.google.protobuf.GeneratedMessageLite.parseFrom(
              DEFAULT_INSTANCE, data);
        }
        public static Product parseFrom(
            byte[] data,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return com.google.protobuf.GeneratedMessageLite.parseFrom(
              DEFAULT_INSTANCE, data, extensionRegistry);
        }
        public static Product parseFrom(java.io.InputStream input)
            throws java.io.IOException {
          return com.google.protobuf.GeneratedMessageLite.parseFrom(
              DEFAULT_INSTANCE, input);
        }
        public static Product parseFrom(
            java.io.InputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
          return com.google.protobuf.GeneratedMessageLite.parseFrom(
              DEFAULT_INSTANCE, input, extensionRegistry);
        }
        public static Product parseDelimitedFrom(java.io.InputStream input)
            throws java.io.IOException {
          return parseDelimitedFrom(DEFAULT_INSTANCE, input);
        }
        public static Product parseDelimitedFrom(
            java.io.InputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
          return parseDelimitedFrom(DEFAULT_INSTANCE, input, extensionRegistry);
        }
        public static Product parseFrom(
            com.google.protobuf.CodedInputStream input)
            throws java.io.IOException {
          return com.google.protobuf.GeneratedMessageLite.parseFrom(
              DEFAULT_INSTANCE, input);
        }
        public static Product parseFrom(
            com.google.protobuf.CodedInputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
          return com.google.protobuf.GeneratedMessageLite.parseFrom(
              DEFAULT_INSTANCE, input, extensionRegistry);
        }

        public static Builder newBuilder() {
          return DEFAULT_INSTANCE.toBuilder();
        }
        public static Builder newBuilder(Product prototype) {
          return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
        }

        /**
         * Protobuf type {@code GearMetadata.GearData.Product}
         */
        public static final class Builder extends
            com.google.protobuf.GeneratedMessageLite.Builder<
              Product, Builder> implements
            // @@protoc_insertion_point(builder_implements:GearMetadata.GearData.Product)
            ProductOrBuilder {
          // Construct using JQGearMetadata.GearMetadata.GearData.Product.newBuilder()
          private Builder() {
            super(DEFAULT_INSTANCE);
          }


          /**
           * <code>optional string id = 1;</code>
           */
          public String getId() {
            return instance.getId();
          }
          /**
           * <code>optional string id = 1;</code>
           */
          public com.google.protobuf.ByteString
              getIdBytes() {
            return instance.getIdBytes();
          }
          /**
           * <code>optional string id = 1;</code>
           */
          public Builder setId(
              String value) {
            copyOnWrite();
            instance.setId(value);
            return this;
          }
          /**
           * <code>optional string id = 1;</code>
           */
          public Builder clearId() {
            copyOnWrite();
            instance.clearId();
            return this;
          }
          /**
           * <code>optional string id = 1;</code>
           */
          public Builder setIdBytes(
              com.google.protobuf.ByteString value) {
            copyOnWrite();
            instance.setIdBytes(value);
            return this;
          }

          /**
           * <code>optional string name = 2;</code>
           */
          public String getName() {
            return instance.getName();
          }
          /**
           * <code>optional string name = 2;</code>
           */
          public com.google.protobuf.ByteString
              getNameBytes() {
            return instance.getNameBytes();
          }
          /**
           * <code>optional string name = 2;</code>
           */
          public Builder setName(
              String value) {
            copyOnWrite();
            instance.setName(value);
            return this;
          }
          /**
           * <code>optional string name = 2;</code>
           */
          public Builder clearName() {
            copyOnWrite();
            instance.clearName();
            return this;
          }
          /**
           * <code>optional string name = 2;</code>
           */
          public Builder setNameBytes(
              com.google.protobuf.ByteString value) {
            copyOnWrite();
            instance.setNameBytes(value);
            return this;
          }

          /**
           * <code>optional string image = 3;</code>
           */
          public String getImage() {
            return instance.getImage();
          }
          /**
           * <code>optional string image = 3;</code>
           */
          public com.google.protobuf.ByteString
              getImageBytes() {
            return instance.getImageBytes();
          }
          /**
           * <code>optional string image = 3;</code>
           */
          public Builder setImage(
              String value) {
            copyOnWrite();
            instance.setImage(value);
            return this;
          }
          /**
           * <code>optional string image = 3;</code>
           */
          public Builder clearImage() {
            copyOnWrite();
            instance.clearImage();
            return this;
          }
          /**
           * <code>optional string image = 3;</code>
           */
          public Builder setImageBytes(
              com.google.protobuf.ByteString value) {
            copyOnWrite();
            instance.setImageBytes(value);
            return this;
          }

          /**
           * <code>repeated .GearMetadata.Capability capabilities = 4;</code>
           */
          public java.util.List<Capability> getCapabilitiesList() {
            return instance.getCapabilitiesList();
          }
          /**
           * <code>repeated .GearMetadata.Capability capabilities = 4;</code>
           */
          public int getCapabilitiesCount() {
            return instance.getCapabilitiesCount();
          }
          /**
           * <code>repeated .GearMetadata.Capability capabilities = 4;</code>
           */
          public Capability getCapabilities(int index) {
            return instance.getCapabilities(index);
          }
          /**
           * <code>repeated .GearMetadata.Capability capabilities = 4;</code>
           */
          public Builder setCapabilities(
              int index, Capability value) {
            copyOnWrite();
            instance.setCapabilities(index, value);
            return this;
          }
          /**
           * <code>repeated .GearMetadata.Capability capabilities = 4;</code>
           */
          public Builder addCapabilities(Capability value) {
            copyOnWrite();
            instance.addCapabilities(value);
            return this;
          }
          /**
           * <code>repeated .GearMetadata.Capability capabilities = 4;</code>
           */
          public Builder addAllCapabilities(
              Iterable<? extends Capability> values) {
            copyOnWrite();
            instance.addAllCapabilities(values);  return this;
          }
          /**
           * <code>repeated .GearMetadata.Capability capabilities = 4;</code>
           */
          public Builder clearCapabilities() {
            copyOnWrite();
            instance.clearCapabilities();
            return this;
          }
          /**
           * <code>repeated .GearMetadata.Capability capabilities = 4;</code>
           */
          public java.util.List<Integer>
          getCapabilitiesValueList() {
            return java.util.Collections.unmodifiableList(
                instance.getCapabilitiesValueList());
          }
          /**
           * <code>repeated .GearMetadata.Capability capabilities = 4;</code>
           */
          public int getCapabilitiesValue(int index) {
            return instance.getCapabilitiesValue(index);
          }
          /**
           * <code>repeated .GearMetadata.Capability capabilities = 4;</code>
           */
          public Builder setCapabilitiesValue(
              int index, int value) {
            copyOnWrite();
            instance.setCapabilitiesValue(index, value);
            return this;
          }
          /**
           * <code>repeated .GearMetadata.Capability capabilities = 4;</code>
           */
          public Builder addCapabilitiesValue(int value) {
            instance.addCapabilitiesValue(value);
            return this;
          }
          /**
           * <code>repeated .GearMetadata.Capability capabilities = 4;</code>
           */
          public Builder addAllCapabilitiesValue(
              Iterable<Integer> values) {
            copyOnWrite();
            instance.addAllCapabilitiesValue(values);
            return this;
          }

          // @@protoc_insertion_point(builder_scope:GearMetadata.GearData.Product)
        }
        protected final Object dynamicMethod(
            MethodToInvoke method,
            Object arg0, Object arg1) {
          switch (method) {
            case NEW_MUTABLE_INSTANCE: {
              return new Product();
            }
            case IS_INITIALIZED: {
              return DEFAULT_INSTANCE;
            }
            case MAKE_IMMUTABLE: {
              capabilities_.makeImmutable();
              return null;
            }
            case NEW_BUILDER: {
              return new Builder();
            }
            case VISIT: {
              Visitor visitor = (Visitor) arg0;
              Product other = (Product) arg1;
              id_ = visitor.visitString(!id_.isEmpty(), id_,
                  !other.id_.isEmpty(), other.id_);
              name_ = visitor.visitString(!name_.isEmpty(), name_,
                  !other.name_.isEmpty(), other.name_);
              image_ = visitor.visitString(!image_.isEmpty(), image_,
                  !other.image_.isEmpty(), other.image_);
              capabilities_= visitor.visitIntList(capabilities_, other.capabilities_);
              if (visitor == MergeFromVisitor
                  .INSTANCE) {
                bitField0_ |= other.bitField0_;
              }
              return this;
            }
            case MERGE_FROM_STREAM: {
              com.google.protobuf.CodedInputStream input =
                  (com.google.protobuf.CodedInputStream) arg0;
              com.google.protobuf.ExtensionRegistryLite extensionRegistry =
                  (com.google.protobuf.ExtensionRegistryLite) arg1;
              try {
                boolean done = false;
                while (!done) {
                  int tag = input.readTag();
                  switch (tag) {
                    case 0:
                      done = true;
                      break;
                    default: {
                      if (!input.skipField(tag)) {
                        done = true;
                      }
                      break;
                    }
                    case 10: {
                      String s = input.readStringRequireUtf8();

                      id_ = s;
                      break;
                    }
                    case 18: {
                      String s = input.readStringRequireUtf8();

                      name_ = s;
                      break;
                    }
                    case 26: {
                      String s = input.readStringRequireUtf8();

                      image_ = s;
                      break;
                    }
                    case 32: {
                      if (!capabilities_.isModifiable()) {
                        capabilities_ =
                            com.google.protobuf.GeneratedMessageLite.mutableCopy(capabilities_);
                      }
                      capabilities_.addInt(input.readEnum());
                      break;
                    }
                    case 34: {
                      if (!capabilities_.isModifiable()) {
                        capabilities_ =
                            com.google.protobuf.GeneratedMessageLite.mutableCopy(capabilities_);
                      }
                      int length = input.readRawVarint32();
                      int oldLimit = input.pushLimit(length);
                      while(input.getBytesUntilLimit() > 0) {
                        capabilities_.addInt(input.readEnum());
                      }
                      input.popLimit(oldLimit);
                      break;
                    }
                  }
                }
              } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                throw new RuntimeException(e.setUnfinishedMessage(this));
              } catch (java.io.IOException e) {
                throw new RuntimeException(
                    new com.google.protobuf.InvalidProtocolBufferException(
                        e.getMessage()).setUnfinishedMessage(this));
              } finally {
              }
            }
            case GET_DEFAULT_INSTANCE: {
              return DEFAULT_INSTANCE;
            }
            case GET_PARSER: {
              if (PARSER == null) {    synchronized (Product.class) {
                  if (PARSER == null) {
                    PARSER = new DefaultInstanceBasedParser(DEFAULT_INSTANCE);
                  }
                }
              }
              return PARSER;
            }
          }
          throw new UnsupportedOperationException();
        }


        // @@protoc_insertion_point(class_scope:GearMetadata.GearData.Product)
        private static final Product DEFAULT_INSTANCE;
        static {
          DEFAULT_INSTANCE = new Product();
          DEFAULT_INSTANCE.makeImmutable();
        }

        public static Product getDefaultInstance() {
          return DEFAULT_INSTANCE;
        }

        private static volatile com.google.protobuf.Parser<Product> PARSER;

        public static com.google.protobuf.Parser<Product> parser() {
          return DEFAULT_INSTANCE.getParserForType();
        }
      }

      public interface VendorOrBuilder extends
          // @@protoc_insertion_point(interface_extends:GearMetadata.GearData.Vendor)
          com.google.protobuf.MessageLiteOrBuilder {

        /**
         * <code>optional string id = 1;</code>
         */
        String getId();
        /**
         * <code>optional string id = 1;</code>
         */
        com.google.protobuf.ByteString
            getIdBytes();

        /**
         * <code>optional string name = 2;</code>
         */
        String getName();
        /**
         * <code>optional string name = 2;</code>
         */
        com.google.protobuf.ByteString
            getNameBytes();

        /**
         * <code>repeated .GearMetadata.GearData.Product products = 3;</code>
         */
        java.util.List<Product>
            getProductsList();
        /**
         * <code>repeated .GearMetadata.GearData.Product products = 3;</code>
         */
        Product getProducts(int index);
        /**
         * <code>repeated .GearMetadata.GearData.Product products = 3;</code>
         */
        int getProductsCount();
      }
      /**
       * Protobuf type {@code GearMetadata.GearData.Vendor}
       */
      public  static final class Vendor extends
          com.google.protobuf.GeneratedMessageLite<
              Vendor, Vendor.Builder> implements
          // @@protoc_insertion_point(message_implements:GearMetadata.GearData.Vendor)
          VendorOrBuilder {
        private Vendor() {
          id_ = "";
          name_ = "";
          products_ = emptyProtobufList();
        }
        private int bitField0_;
        public static final int ID_FIELD_NUMBER = 1;
        private String id_;
        /**
         * <code>optional string id = 1;</code>
         */
        public String getId() {
          return id_;
        }
        /**
         * <code>optional string id = 1;</code>
         */
        public com.google.protobuf.ByteString
            getIdBytes() {
          return com.google.protobuf.ByteString.copyFromUtf8(id_);
        }
        /**
         * <code>optional string id = 1;</code>
         */
        private void setId(
            String value) {
          if (value == null) {
    throw new NullPointerException();
  }
  
          id_ = value;
        }
        /**
         * <code>optional string id = 1;</code>
         */
        private void clearId() {
          
          id_ = getDefaultInstance().getId();
        }
        /**
         * <code>optional string id = 1;</code>
         */
        private void setIdBytes(
            com.google.protobuf.ByteString value) {
          if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
          
          id_ = value.toStringUtf8();
        }

        public static final int NAME_FIELD_NUMBER = 2;
        private String name_;
        /**
         * <code>optional string name = 2;</code>
         */
        public String getName() {
          return name_;
        }
        /**
         * <code>optional string name = 2;</code>
         */
        public com.google.protobuf.ByteString
            getNameBytes() {
          return com.google.protobuf.ByteString.copyFromUtf8(name_);
        }
        /**
         * <code>optional string name = 2;</code>
         */
        private void setName(
            String value) {
          if (value == null) {
    throw new NullPointerException();
  }
  
          name_ = value;
        }
        /**
         * <code>optional string name = 2;</code>
         */
        private void clearName() {
          
          name_ = getDefaultInstance().getName();
        }
        /**
         * <code>optional string name = 2;</code>
         */
        private void setNameBytes(
            com.google.protobuf.ByteString value) {
          if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
          
          name_ = value.toStringUtf8();
        }

        public static final int PRODUCTS_FIELD_NUMBER = 3;
        private com.google.protobuf.Internal.ProtobufList<Product> products_;
        /**
         * <code>repeated .GearMetadata.GearData.Product products = 3;</code>
         */
        public java.util.List<Product> getProductsList() {
          return products_;
        }
        /**
         * <code>repeated .GearMetadata.GearData.Product products = 3;</code>
         */
        public java.util.List<? extends ProductOrBuilder>
            getProductsOrBuilderList() {
          return products_;
        }
        /**
         * <code>repeated .GearMetadata.GearData.Product products = 3;</code>
         */
        public int getProductsCount() {
          return products_.size();
        }
        /**
         * <code>repeated .GearMetadata.GearData.Product products = 3;</code>
         */
        public Product getProducts(int index) {
          return products_.get(index);
        }
        /**
         * <code>repeated .GearMetadata.GearData.Product products = 3;</code>
         */
        public ProductOrBuilder getProductsOrBuilder(
            int index) {
          return products_.get(index);
        }
        private void ensureProductsIsMutable() {
          if (!products_.isModifiable()) {
            products_ =
                com.google.protobuf.GeneratedMessageLite.mutableCopy(products_);
           }
        }

        /**
         * <code>repeated .GearMetadata.GearData.Product products = 3;</code>
         */
        private void setProducts(
            int index, Product value) {
          if (value == null) {
            throw new NullPointerException();
          }
          ensureProductsIsMutable();
          products_.set(index, value);
        }
        /**
         * <code>repeated .GearMetadata.GearData.Product products = 3;</code>
         */
        private void setProducts(
            int index, Product.Builder builderForValue) {
          ensureProductsIsMutable();
          products_.set(index, builderForValue.build());
        }
        /**
         * <code>repeated .GearMetadata.GearData.Product products = 3;</code>
         */
        private void addProducts(Product value) {
          if (value == null) {
            throw new NullPointerException();
          }
          ensureProductsIsMutable();
          products_.add(value);
        }
        /**
         * <code>repeated .GearMetadata.GearData.Product products = 3;</code>
         */
        private void addProducts(
            int index, Product value) {
          if (value == null) {
            throw new NullPointerException();
          }
          ensureProductsIsMutable();
          products_.add(index, value);
        }
        /**
         * <code>repeated .GearMetadata.GearData.Product products = 3;</code>
         */
        private void addProducts(
            Product.Builder builderForValue) {
          ensureProductsIsMutable();
          products_.add(builderForValue.build());
        }
        /**
         * <code>repeated .GearMetadata.GearData.Product products = 3;</code>
         */
        private void addProducts(
            int index, Product.Builder builderForValue) {
          ensureProductsIsMutable();
          products_.add(index, builderForValue.build());
        }
        /**
         * <code>repeated .GearMetadata.GearData.Product products = 3;</code>
         */
        private void addAllProducts(
            Iterable<? extends Product> values) {
          ensureProductsIsMutable();
          com.google.protobuf.AbstractMessageLite.addAll(
              values, products_);
        }
        /**
         * <code>repeated .GearMetadata.GearData.Product products = 3;</code>
         */
        private void clearProducts() {
          products_ = emptyProtobufList();
        }
        /**
         * <code>repeated .GearMetadata.GearData.Product products = 3;</code>
         */
        private void removeProducts(int index) {
          ensureProductsIsMutable();
          products_.remove(index);
        }

        public void writeTo(com.google.protobuf.CodedOutputStream output)
                            throws java.io.IOException {
          if (!id_.isEmpty()) {
            output.writeString(1, getId());
          }
          if (!name_.isEmpty()) {
            output.writeString(2, getName());
          }
          for (int i = 0; i < products_.size(); i++) {
            output.writeMessage(3, products_.get(i));
          }
        }

        public int getSerializedSize() {
          int size = memoizedSerializedSize;
          if (size != -1) return size;

          size = 0;
          if (!id_.isEmpty()) {
            size += com.google.protobuf.CodedOutputStream
              .computeStringSize(1, getId());
          }
          if (!name_.isEmpty()) {
            size += com.google.protobuf.CodedOutputStream
              .computeStringSize(2, getName());
          }
          for (int i = 0; i < products_.size(); i++) {
            size += com.google.protobuf.CodedOutputStream
              .computeMessageSize(3, products_.get(i));
          }
          memoizedSerializedSize = size;
          return size;
        }

        public static Vendor parseFrom(
            com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return com.google.protobuf.GeneratedMessageLite.parseFrom(
              DEFAULT_INSTANCE, data);
        }
        public static Vendor parseFrom(
            com.google.protobuf.ByteString data,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return com.google.protobuf.GeneratedMessageLite.parseFrom(
              DEFAULT_INSTANCE, data, extensionRegistry);
        }
        public static Vendor parseFrom(byte[] data)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return com.google.protobuf.GeneratedMessageLite.parseFrom(
              DEFAULT_INSTANCE, data);
        }
        public static Vendor parseFrom(
            byte[] data,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return com.google.protobuf.GeneratedMessageLite.parseFrom(
              DEFAULT_INSTANCE, data, extensionRegistry);
        }
        public static Vendor parseFrom(java.io.InputStream input)
            throws java.io.IOException {
          return com.google.protobuf.GeneratedMessageLite.parseFrom(
              DEFAULT_INSTANCE, input);
        }
        public static Vendor parseFrom(
            java.io.InputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
          return com.google.protobuf.GeneratedMessageLite.parseFrom(
              DEFAULT_INSTANCE, input, extensionRegistry);
        }
        public static Vendor parseDelimitedFrom(java.io.InputStream input)
            throws java.io.IOException {
          return parseDelimitedFrom(DEFAULT_INSTANCE, input);
        }
        public static Vendor parseDelimitedFrom(
            java.io.InputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
          return parseDelimitedFrom(DEFAULT_INSTANCE, input, extensionRegistry);
        }
        public static Vendor parseFrom(
            com.google.protobuf.CodedInputStream input)
            throws java.io.IOException {
          return com.google.protobuf.GeneratedMessageLite.parseFrom(
              DEFAULT_INSTANCE, input);
        }
        public static Vendor parseFrom(
            com.google.protobuf.CodedInputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
          return com.google.protobuf.GeneratedMessageLite.parseFrom(
              DEFAULT_INSTANCE, input, extensionRegistry);
        }

        public static Builder newBuilder() {
          return DEFAULT_INSTANCE.toBuilder();
        }
        public static Builder newBuilder(Vendor prototype) {
          return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
        }

        /**
         * Protobuf type {@code GearMetadata.GearData.Vendor}
         */
        public static final class Builder extends
            com.google.protobuf.GeneratedMessageLite.Builder<
              Vendor, Builder> implements
            // @@protoc_insertion_point(builder_implements:GearMetadata.GearData.Vendor)
            VendorOrBuilder {
          // Construct using JQGearMetadata.GearMetadata.GearData.Vendor.newBuilder()
          private Builder() {
            super(DEFAULT_INSTANCE);
          }


          /**
           * <code>optional string id = 1;</code>
           */
          public String getId() {
            return instance.getId();
          }
          /**
           * <code>optional string id = 1;</code>
           */
          public com.google.protobuf.ByteString
              getIdBytes() {
            return instance.getIdBytes();
          }
          /**
           * <code>optional string id = 1;</code>
           */
          public Builder setId(
              String value) {
            copyOnWrite();
            instance.setId(value);
            return this;
          }
          /**
           * <code>optional string id = 1;</code>
           */
          public Builder clearId() {
            copyOnWrite();
            instance.clearId();
            return this;
          }
          /**
           * <code>optional string id = 1;</code>
           */
          public Builder setIdBytes(
              com.google.protobuf.ByteString value) {
            copyOnWrite();
            instance.setIdBytes(value);
            return this;
          }

          /**
           * <code>optional string name = 2;</code>
           */
          public String getName() {
            return instance.getName();
          }
          /**
           * <code>optional string name = 2;</code>
           */
          public com.google.protobuf.ByteString
              getNameBytes() {
            return instance.getNameBytes();
          }
          /**
           * <code>optional string name = 2;</code>
           */
          public Builder setName(
              String value) {
            copyOnWrite();
            instance.setName(value);
            return this;
          }
          /**
           * <code>optional string name = 2;</code>
           */
          public Builder clearName() {
            copyOnWrite();
            instance.clearName();
            return this;
          }
          /**
           * <code>optional string name = 2;</code>
           */
          public Builder setNameBytes(
              com.google.protobuf.ByteString value) {
            copyOnWrite();
            instance.setNameBytes(value);
            return this;
          }

          /**
           * <code>repeated .GearMetadata.GearData.Product products = 3;</code>
           */
          public java.util.List<Product> getProductsList() {
            return java.util.Collections.unmodifiableList(
                instance.getProductsList());
          }
          /**
           * <code>repeated .GearMetadata.GearData.Product products = 3;</code>
           */
          public int getProductsCount() {
            return instance.getProductsCount();
          }/**
           * <code>repeated .GearMetadata.GearData.Product products = 3;</code>
           */
          public Product getProducts(int index) {
            return instance.getProducts(index);
          }
          /**
           * <code>repeated .GearMetadata.GearData.Product products = 3;</code>
           */
          public Builder setProducts(
              int index, Product value) {
            copyOnWrite();
            instance.setProducts(index, value);
            return this;
          }
          /**
           * <code>repeated .GearMetadata.GearData.Product products = 3;</code>
           */
          public Builder setProducts(
              int index, Product.Builder builderForValue) {
            copyOnWrite();
            instance.setProducts(index, builderForValue);
            return this;
          }
          /**
           * <code>repeated .GearMetadata.GearData.Product products = 3;</code>
           */
          public Builder addProducts(Product value) {
            copyOnWrite();
            instance.addProducts(value);
            return this;
          }
          /**
           * <code>repeated .GearMetadata.GearData.Product products = 3;</code>
           */
          public Builder addProducts(
              int index, Product value) {
            copyOnWrite();
            instance.addProducts(index, value);
            return this;
          }
          /**
           * <code>repeated .GearMetadata.GearData.Product products = 3;</code>
           */
          public Builder addProducts(
              Product.Builder builderForValue) {
            copyOnWrite();
            instance.addProducts(builderForValue);
            return this;
          }
          /**
           * <code>repeated .GearMetadata.GearData.Product products = 3;</code>
           */
          public Builder addProducts(
              int index, Product.Builder builderForValue) {
            copyOnWrite();
            instance.addProducts(index, builderForValue);
            return this;
          }
          /**
           * <code>repeated .GearMetadata.GearData.Product products = 3;</code>
           */
          public Builder addAllProducts(
              Iterable<? extends Product> values) {
            copyOnWrite();
            instance.addAllProducts(values);
            return this;
          }
          /**
           * <code>repeated .GearMetadata.GearData.Product products = 3;</code>
           */
          public Builder clearProducts() {
            copyOnWrite();
            instance.clearProducts();
            return this;
          }
          /**
           * <code>repeated .GearMetadata.GearData.Product products = 3;</code>
           */
          public Builder removeProducts(int index) {
            copyOnWrite();
            instance.removeProducts(index);
            return this;
          }

          // @@protoc_insertion_point(builder_scope:GearMetadata.GearData.Vendor)
        }
        protected final Object dynamicMethod(
            MethodToInvoke method,
            Object arg0, Object arg1) {
          switch (method) {
            case NEW_MUTABLE_INSTANCE: {
              return new Vendor();
            }
            case IS_INITIALIZED: {
              return DEFAULT_INSTANCE;
            }
            case MAKE_IMMUTABLE: {
              products_.makeImmutable();
              return null;
            }
            case NEW_BUILDER: {
              return new Builder();
            }
            case VISIT: {
              Visitor visitor = (Visitor) arg0;
              Vendor other = (Vendor) arg1;
              id_ = visitor.visitString(!id_.isEmpty(), id_,
                  !other.id_.isEmpty(), other.id_);
              name_ = visitor.visitString(!name_.isEmpty(), name_,
                  !other.name_.isEmpty(), other.name_);
              products_= visitor.visitList(products_, other.products_);
              if (visitor == MergeFromVisitor
                  .INSTANCE) {
                bitField0_ |= other.bitField0_;
              }
              return this;
            }
            case MERGE_FROM_STREAM: {
              com.google.protobuf.CodedInputStream input =
                  (com.google.protobuf.CodedInputStream) arg0;
              com.google.protobuf.ExtensionRegistryLite extensionRegistry =
                  (com.google.protobuf.ExtensionRegistryLite) arg1;
              try {
                boolean done = false;
                while (!done) {
                  int tag = input.readTag();
                  switch (tag) {
                    case 0:
                      done = true;
                      break;
                    default: {
                      if (!input.skipField(tag)) {
                        done = true;
                      }
                      break;
                    }
                    case 10: {
                      String s = input.readStringRequireUtf8();

                      id_ = s;
                      break;
                    }
                    case 18: {
                      String s = input.readStringRequireUtf8();

                      name_ = s;
                      break;
                    }
                    case 26: {
                      if (!products_.isModifiable()) {
                        products_ =
                            com.google.protobuf.GeneratedMessageLite.mutableCopy(products_);
                      }
                      products_.add(
                          input.readMessage(Product.parser(), extensionRegistry));
                      break;
                    }
                  }
                }
              } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                throw new RuntimeException(e.setUnfinishedMessage(this));
              } catch (java.io.IOException e) {
                throw new RuntimeException(
                    new com.google.protobuf.InvalidProtocolBufferException(
                        e.getMessage()).setUnfinishedMessage(this));
              } finally {
              }
            }
            case GET_DEFAULT_INSTANCE: {
              return DEFAULT_INSTANCE;
            }
            case GET_PARSER: {
              if (PARSER == null) {    synchronized (Vendor.class) {
                  if (PARSER == null) {
                    PARSER = new DefaultInstanceBasedParser(DEFAULT_INSTANCE);
                  }
                }
              }
              return PARSER;
            }
          }
          throw new UnsupportedOperationException();
        }


        // @@protoc_insertion_point(class_scope:GearMetadata.GearData.Vendor)
        private static final Vendor DEFAULT_INSTANCE;
        static {
          DEFAULT_INSTANCE = new Vendor();
          DEFAULT_INSTANCE.makeImmutable();
        }

        public static Vendor getDefaultInstance() {
          return DEFAULT_INSTANCE;
        }

        private static volatile com.google.protobuf.Parser<Vendor> PARSER;

        public static com.google.protobuf.Parser<Vendor> parser() {
          return DEFAULT_INSTANCE.getParserForType();
        }
      }

      public static final int VENDORS_FIELD_NUMBER = 1;
      private com.google.protobuf.Internal.ProtobufList<Vendor> vendors_;
      /**
       * <code>repeated .GearMetadata.GearData.Vendor vendors = 1;</code>
       */
      public java.util.List<Vendor> getVendorsList() {
        return vendors_;
      }
      /**
       * <code>repeated .GearMetadata.GearData.Vendor vendors = 1;</code>
       */
      public java.util.List<? extends VendorOrBuilder>
          getVendorsOrBuilderList() {
        return vendors_;
      }
      /**
       * <code>repeated .GearMetadata.GearData.Vendor vendors = 1;</code>
       */
      public int getVendorsCount() {
        return vendors_.size();
      }
      /**
       * <code>repeated .GearMetadata.GearData.Vendor vendors = 1;</code>
       */
      public Vendor getVendors(int index) {
        return vendors_.get(index);
      }
      /**
       * <code>repeated .GearMetadata.GearData.Vendor vendors = 1;</code>
       */
      public VendorOrBuilder getVendorsOrBuilder(
          int index) {
        return vendors_.get(index);
      }
      private void ensureVendorsIsMutable() {
        if (!vendors_.isModifiable()) {
          vendors_ =
              com.google.protobuf.GeneratedMessageLite.mutableCopy(vendors_);
         }
      }

      /**
       * <code>repeated .GearMetadata.GearData.Vendor vendors = 1;</code>
       */
      private void setVendors(
          int index, Vendor value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureVendorsIsMutable();
        vendors_.set(index, value);
      }
      /**
       * <code>repeated .GearMetadata.GearData.Vendor vendors = 1;</code>
       */
      private void setVendors(
          int index, Vendor.Builder builderForValue) {
        ensureVendorsIsMutable();
        vendors_.set(index, builderForValue.build());
      }
      /**
       * <code>repeated .GearMetadata.GearData.Vendor vendors = 1;</code>
       */
      private void addVendors(Vendor value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureVendorsIsMutable();
        vendors_.add(value);
      }
      /**
       * <code>repeated .GearMetadata.GearData.Vendor vendors = 1;</code>
       */
      private void addVendors(
          int index, Vendor value) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureVendorsIsMutable();
        vendors_.add(index, value);
      }
      /**
       * <code>repeated .GearMetadata.GearData.Vendor vendors = 1;</code>
       */
      private void addVendors(
          Vendor.Builder builderForValue) {
        ensureVendorsIsMutable();
        vendors_.add(builderForValue.build());
      }
      /**
       * <code>repeated .GearMetadata.GearData.Vendor vendors = 1;</code>
       */
      private void addVendors(
          int index, Vendor.Builder builderForValue) {
        ensureVendorsIsMutable();
        vendors_.add(index, builderForValue.build());
      }
      /**
       * <code>repeated .GearMetadata.GearData.Vendor vendors = 1;</code>
       */
      private void addAllVendors(
          Iterable<? extends Vendor> values) {
        ensureVendorsIsMutable();
        com.google.protobuf.AbstractMessageLite.addAll(
            values, vendors_);
      }
      /**
       * <code>repeated .GearMetadata.GearData.Vendor vendors = 1;</code>
       */
      private void clearVendors() {
        vendors_ = emptyProtobufList();
      }
      /**
       * <code>repeated .GearMetadata.GearData.Vendor vendors = 1;</code>
       */
      private void removeVendors(int index) {
        ensureVendorsIsMutable();
        vendors_.remove(index);
      }

      public void writeTo(com.google.protobuf.CodedOutputStream output)
                          throws java.io.IOException {
        for (int i = 0; i < vendors_.size(); i++) {
          output.writeMessage(1, vendors_.get(i));
        }
      }

      public int getSerializedSize() {
        int size = memoizedSerializedSize;
        if (size != -1) return size;

        size = 0;
        for (int i = 0; i < vendors_.size(); i++) {
          size += com.google.protobuf.CodedOutputStream
            .computeMessageSize(1, vendors_.get(i));
        }
        memoizedSerializedSize = size;
        return size;
      }

      public static GearData parseFrom(
          com.google.protobuf.ByteString data)
          throws com.google.protobuf.InvalidProtocolBufferException {
        return com.google.protobuf.GeneratedMessageLite.parseFrom(
            DEFAULT_INSTANCE, data);
      }
      public static GearData parseFrom(
          com.google.protobuf.ByteString data,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        return com.google.protobuf.GeneratedMessageLite.parseFrom(
            DEFAULT_INSTANCE, data, extensionRegistry);
      }
      public static GearData parseFrom(byte[] data)
          throws com.google.protobuf.InvalidProtocolBufferException {
        return com.google.protobuf.GeneratedMessageLite.parseFrom(
            DEFAULT_INSTANCE, data);
      }
      public static GearData parseFrom(
          byte[] data,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        return com.google.protobuf.GeneratedMessageLite.parseFrom(
            DEFAULT_INSTANCE, data, extensionRegistry);
      }
      public static GearData parseFrom(java.io.InputStream input)
          throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageLite.parseFrom(
            DEFAULT_INSTANCE, input);
      }
      public static GearData parseFrom(
          java.io.InputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageLite.parseFrom(
            DEFAULT_INSTANCE, input, extensionRegistry);
      }
      public static GearData parseDelimitedFrom(java.io.InputStream input)
          throws java.io.IOException {
        return parseDelimitedFrom(DEFAULT_INSTANCE, input);
      }
      public static GearData parseDelimitedFrom(
          java.io.InputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        return parseDelimitedFrom(DEFAULT_INSTANCE, input, extensionRegistry);
      }
      public static GearData parseFrom(
          com.google.protobuf.CodedInputStream input)
          throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageLite.parseFrom(
            DEFAULT_INSTANCE, input);
      }
      public static GearData parseFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageLite.parseFrom(
            DEFAULT_INSTANCE, input, extensionRegistry);
      }

      public static Builder newBuilder() {
        return DEFAULT_INSTANCE.toBuilder();
      }
      public static Builder newBuilder(GearData prototype) {
        return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
      }

      /**
       * Protobuf type {@code GearMetadata.GearData}
       */
      public static final class Builder extends
          com.google.protobuf.GeneratedMessageLite.Builder<
            GearData, Builder> implements
          // @@protoc_insertion_point(builder_implements:GearMetadata.GearData)
          GearDataOrBuilder {
        // Construct using JQGearMetadata.GearMetadata.GearData.newBuilder()
        private Builder() {
          super(DEFAULT_INSTANCE);
        }


        /**
         * <code>repeated .GearMetadata.GearData.Vendor vendors = 1;</code>
         */
        public java.util.List<Vendor> getVendorsList() {
          return java.util.Collections.unmodifiableList(
              instance.getVendorsList());
        }
        /**
         * <code>repeated .GearMetadata.GearData.Vendor vendors = 1;</code>
         */
        public int getVendorsCount() {
          return instance.getVendorsCount();
        }/**
         * <code>repeated .GearMetadata.GearData.Vendor vendors = 1;</code>
         */
        public Vendor getVendors(int index) {
          return instance.getVendors(index);
        }
        /**
         * <code>repeated .GearMetadata.GearData.Vendor vendors = 1;</code>
         */
        public Builder setVendors(
            int index, Vendor value) {
          copyOnWrite();
          instance.setVendors(index, value);
          return this;
        }
        /**
         * <code>repeated .GearMetadata.GearData.Vendor vendors = 1;</code>
         */
        public Builder setVendors(
            int index, Vendor.Builder builderForValue) {
          copyOnWrite();
          instance.setVendors(index, builderForValue);
          return this;
        }
        /**
         * <code>repeated .GearMetadata.GearData.Vendor vendors = 1;</code>
         */
        public Builder addVendors(Vendor value) {
          copyOnWrite();
          instance.addVendors(value);
          return this;
        }
        /**
         * <code>repeated .GearMetadata.GearData.Vendor vendors = 1;</code>
         */
        public Builder addVendors(
            int index, Vendor value) {
          copyOnWrite();
          instance.addVendors(index, value);
          return this;
        }
        /**
         * <code>repeated .GearMetadata.GearData.Vendor vendors = 1;</code>
         */
        public Builder addVendors(
            Vendor.Builder builderForValue) {
          copyOnWrite();
          instance.addVendors(builderForValue);
          return this;
        }
        /**
         * <code>repeated .GearMetadata.GearData.Vendor vendors = 1;</code>
         */
        public Builder addVendors(
            int index, Vendor.Builder builderForValue) {
          copyOnWrite();
          instance.addVendors(index, builderForValue);
          return this;
        }
        /**
         * <code>repeated .GearMetadata.GearData.Vendor vendors = 1;</code>
         */
        public Builder addAllVendors(
            Iterable<? extends Vendor> values) {
          copyOnWrite();
          instance.addAllVendors(values);
          return this;
        }
        /**
         * <code>repeated .GearMetadata.GearData.Vendor vendors = 1;</code>
         */
        public Builder clearVendors() {
          copyOnWrite();
          instance.clearVendors();
          return this;
        }
        /**
         * <code>repeated .GearMetadata.GearData.Vendor vendors = 1;</code>
         */
        public Builder removeVendors(int index) {
          copyOnWrite();
          instance.removeVendors(index);
          return this;
        }

        // @@protoc_insertion_point(builder_scope:GearMetadata.GearData)
      }
      protected final Object dynamicMethod(
          MethodToInvoke method,
          Object arg0, Object arg1) {
        switch (method) {
          case NEW_MUTABLE_INSTANCE: {
            return new GearData();
          }
          case IS_INITIALIZED: {
            return DEFAULT_INSTANCE;
          }
          case MAKE_IMMUTABLE: {
            vendors_.makeImmutable();
            return null;
          }
          case NEW_BUILDER: {
            return new Builder();
          }
          case VISIT: {
            Visitor visitor = (Visitor) arg0;
            GearData other = (GearData) arg1;
            vendors_= visitor.visitList(vendors_, other.vendors_);
            if (visitor == MergeFromVisitor
                .INSTANCE) {
            }
            return this;
          }
          case MERGE_FROM_STREAM: {
            com.google.protobuf.CodedInputStream input =
                (com.google.protobuf.CodedInputStream) arg0;
            com.google.protobuf.ExtensionRegistryLite extensionRegistry =
                (com.google.protobuf.ExtensionRegistryLite) arg1;
            try {
              boolean done = false;
              while (!done) {
                int tag = input.readTag();
                switch (tag) {
                  case 0:
                    done = true;
                    break;
                  default: {
                    if (!input.skipField(tag)) {
                      done = true;
                    }
                    break;
                  }
                  case 10: {
                    if (!vendors_.isModifiable()) {
                      vendors_ =
                          com.google.protobuf.GeneratedMessageLite.mutableCopy(vendors_);
                    }
                    vendors_.add(
                        input.readMessage(Vendor.parser(), extensionRegistry));
                    break;
                  }
                }
              }
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
              throw new RuntimeException(e.setUnfinishedMessage(this));
            } catch (java.io.IOException e) {
              throw new RuntimeException(
                  new com.google.protobuf.InvalidProtocolBufferException(
                      e.getMessage()).setUnfinishedMessage(this));
            } finally {
            }
          }
          case GET_DEFAULT_INSTANCE: {
            return DEFAULT_INSTANCE;
          }
          case GET_PARSER: {
            if (PARSER == null) {    synchronized (GearData.class) {
                if (PARSER == null) {
                  PARSER = new DefaultInstanceBasedParser(DEFAULT_INSTANCE);
                }
              }
            }
            return PARSER;
          }
        }
        throw new UnsupportedOperationException();
      }


      // @@protoc_insertion_point(class_scope:GearMetadata.GearData)
      private static final GearData DEFAULT_INSTANCE;
      static {
        DEFAULT_INSTANCE = new GearData();
        DEFAULT_INSTANCE.makeImmutable();
      }

      public static GearData getDefaultInstance() {
        return DEFAULT_INSTANCE;
      }

      private static volatile com.google.protobuf.Parser<GearData> PARSER;

      public static com.google.protobuf.Parser<GearData> parser() {
        return DEFAULT_INSTANCE.getParserForType();
      }
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
    }

    public int getSerializedSize() {
      int size = memoizedSerializedSize;
      if (size != -1) return size;

      size = 0;
      memoizedSerializedSize = size;
      return size;
    }

    public static GearMetadata parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
          DEFAULT_INSTANCE, data);
    }
    public static GearMetadata parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
          DEFAULT_INSTANCE, data, extensionRegistry);
    }
    public static GearMetadata parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
          DEFAULT_INSTANCE, data);
    }
    public static GearMetadata parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
          DEFAULT_INSTANCE, data, extensionRegistry);
    }
    public static GearMetadata parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
          DEFAULT_INSTANCE, input);
    }
    public static GearMetadata parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
          DEFAULT_INSTANCE, input, extensionRegistry);
    }
    public static GearMetadata parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return parseDelimitedFrom(DEFAULT_INSTANCE, input);
    }
    public static GearMetadata parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return parseDelimitedFrom(DEFAULT_INSTANCE, input, extensionRegistry);
    }
    public static GearMetadata parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
          DEFAULT_INSTANCE, input);
    }
    public static GearMetadata parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
          DEFAULT_INSTANCE, input, extensionRegistry);
    }

    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }
    public static Builder newBuilder(GearMetadata prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    /**
     * Protobuf type {@code GearMetadata}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessageLite.Builder<
          GearMetadata, Builder> implements
        // @@protoc_insertion_point(builder_implements:GearMetadata)
        GearMetadataOrBuilder {
      // Construct using JQGearMetadata.GearMetadata.newBuilder()
      private Builder() {
        super(DEFAULT_INSTANCE);
      }


      // @@protoc_insertion_point(builder_scope:GearMetadata)
    }
    protected final Object dynamicMethod(
        MethodToInvoke method,
        Object arg0, Object arg1) {
      switch (method) {
        case NEW_MUTABLE_INSTANCE: {
          return new GearMetadata();
        }
        case IS_INITIALIZED: {
          return DEFAULT_INSTANCE;
        }
        case MAKE_IMMUTABLE: {
          return null;
        }
        case NEW_BUILDER: {
          return new Builder();
        }
        case VISIT: {
          Visitor visitor = (Visitor) arg0;
          GearMetadata other = (GearMetadata) arg1;
          if (visitor == MergeFromVisitor
              .INSTANCE) {
          }
          return this;
        }
        case MERGE_FROM_STREAM: {
          com.google.protobuf.CodedInputStream input =
              (com.google.protobuf.CodedInputStream) arg0;
          com.google.protobuf.ExtensionRegistryLite extensionRegistry =
              (com.google.protobuf.ExtensionRegistryLite) arg1;
          try {
            boolean done = false;
            while (!done) {
              int tag = input.readTag();
              switch (tag) {
                case 0:
                  done = true;
                  break;
                default: {
                  if (!input.skipField(tag)) {
                    done = true;
                  }
                  break;
                }
              }
            }
          } catch (com.google.protobuf.InvalidProtocolBufferException e) {
            throw new RuntimeException(e.setUnfinishedMessage(this));
          } catch (java.io.IOException e) {
            throw new RuntimeException(
                new com.google.protobuf.InvalidProtocolBufferException(
                    e.getMessage()).setUnfinishedMessage(this));
          } finally {
          }
        }
        case GET_DEFAULT_INSTANCE: {
          return DEFAULT_INSTANCE;
        }
        case GET_PARSER: {
          if (PARSER == null) {    synchronized (GearMetadata.class) {
              if (PARSER == null) {
                PARSER = new DefaultInstanceBasedParser(DEFAULT_INSTANCE);
              }
            }
          }
          return PARSER;
        }
      }
      throw new UnsupportedOperationException();
    }


    // @@protoc_insertion_point(class_scope:GearMetadata)
    private static final GearMetadata DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new GearMetadata();
      DEFAULT_INSTANCE.makeImmutable();
    }

    public static GearMetadata getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static volatile com.google.protobuf.Parser<GearMetadata> PARSER;

    public static com.google.protobuf.Parser<GearMetadata> parser() {
      return DEFAULT_INSTANCE.getParserForType();
    }
  }


  static {
  }

  // @@protoc_insertion_point(outer_class_scope)
}
