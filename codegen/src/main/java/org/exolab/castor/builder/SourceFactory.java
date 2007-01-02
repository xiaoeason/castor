/**
 * Redistribution and use of this software and associated documentation
 * ("Software"), with or without modification, are permitted provided
 * that the following conditions are met:
 *
 * 1. Redistributions of source code must retain copyright
 *    statements and notices.  Redistributions must also contain a
 *    copy of this document.
 *
 * 2. Redistributions in binary form must reproduce the
 *    above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other
 *    materials provided with the distribution.
 *
 * 3. The name "Exolab" must not be used to endorse or promote
 *    products derived from this Software without prior written
 *    permission of Intalio, Inc.  For written permission,
 *    please contact info@exolab.org.
 *
 * 4. Products derived from this Software may not be called "Exolab"
 *    nor may "Exolab" appear in their names without prior written
 *    permission of Intalio, Inc. Exolab is a registered
 *    trademark of Intalio, Inc.
 *
 * 5. Due credit should be given to the Exolab Project
 *    (http://www.exolab.org/).
 *
 * THIS SOFTWARE IS PROVIDED BY INTALIO, INC. AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
 * INTALIO, INC. OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Copyright 1999-2004 (C) Intalio Inc. All Rights Reserved.
 *
 * This file was originally developed by Keith Visco during the course
 * of employment at Intalio Inc.
 * Portions of this file developed by Keith Visco after Jan 19 2005 are
 * Copyright (C) 2005 Keith Visco. All Rights Reserverd.
 *
 * $Id$
 */
package org.exolab.castor.builder;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.exolab.castor.builder.binding.ExtendedBinding;
import org.exolab.castor.builder.binding.XMLBindingComponent;
import org.exolab.castor.builder.info.ClassInfo;
import org.exolab.castor.builder.info.FieldInfo;
import org.exolab.castor.builder.info.XMLInfo;
import org.exolab.castor.builder.types.XSClass;
import org.exolab.castor.builder.types.XSString;
import org.exolab.castor.builder.types.XSType;
import org.exolab.castor.xml.JavaNaming;
import org.exolab.castor.xml.schema.Annotated;
import org.exolab.castor.xml.schema.Annotation;
import org.exolab.castor.xml.schema.AttributeDecl;
import org.exolab.castor.xml.schema.AttributeGroupDecl;
import org.exolab.castor.xml.schema.ComplexType;
import org.exolab.castor.xml.schema.ContentModelGroup;
import org.exolab.castor.xml.schema.ContentType;
import org.exolab.castor.xml.schema.Documentation;
import org.exolab.castor.xml.schema.ElementDecl;
import org.exolab.castor.xml.schema.Facet;
import org.exolab.castor.xml.schema.Group;
import org.exolab.castor.xml.schema.ModelGroup;
import org.exolab.castor.xml.schema.Order;
import org.exolab.castor.xml.schema.Particle;
import org.exolab.castor.xml.schema.Schema;
import org.exolab.castor.xml.schema.SimpleContent;
import org.exolab.castor.xml.schema.SimpleType;
import org.exolab.castor.xml.schema.SimpleTypesFactory;
import org.exolab.castor.xml.schema.Structure;
import org.exolab.castor.xml.schema.Wildcard;
import org.exolab.castor.xml.schema.XMLType;
import org.exolab.javasource.JAnnotation;
import org.exolab.javasource.JAnnotationType;
import org.exolab.javasource.JArrayType;
import org.exolab.javasource.JClass;
import org.exolab.javasource.JCollectionType;
import org.exolab.javasource.JConstructor;
import org.exolab.javasource.JDocComment;
import org.exolab.javasource.JDocDescriptor;
import org.exolab.javasource.JField;
import org.exolab.javasource.JMethod;
import org.exolab.javasource.JParameter;
import org.exolab.javasource.JPrimitiveType;
import org.exolab.javasource.JSourceCode;
import org.exolab.javasource.JType;

/**
 * Creates the Java Source classes for Schema components.
 *
 * @author <a href="mailto:keith AT kvisco DOT com">Keith Visco</a>
 * @author <a href="mailto:blandin@intalio.com">Arnaud Blandin</a>
 * @version $Revision$ $Date: 2006-04-25 15:08:23 -0600 (Tue, 25 Apr 2006) $
 */
public final class SourceFactory extends BaseFactory {
    private static final String ENUM_ACCESS_INTERFACE =
        "org.exolab.castor.types.EnumeratedTypeAccess";

    private static final short  BASE_TYPE_ENUMERATION   = 0;
    private static final short  OBJECT_TYPE_ENUMERATION = 1;

    private static final String CLASS_METHOD_SUFFIX     = "Class";
    private static final String CLASS_KEYWORD           = "class";
    private static final String ITEM_NAME               = "Item";

    /**  The current Binding for which we are creating classes. */
    private ExtendedBinding _binding = null;
    /** The member factory. */
    private MemberFactory memberFactory = null;

    private short enumerationType = OBJECT_TYPE_ENUMERATION;

    /**
     * A flag indicating whether or not to generate XML marshalling framework
     * specific methods.
     */
    private boolean _createMarshalMethods = true;

    /**
     * A flag indicating whether or not to implement CastorTestable (used by the
     * Castor Testing Framework).
     */
    private boolean _testable = false;
    /** A flag indicating that SAX1 should be used when generating the source. */
    private boolean _sax1 = false;
    /** The TypeConversion instance to use for mapping SimpleTypes into XSTypes. */
    private TypeConversion _typeConversion = null;
    /** Enumeration factory used to create code for enumerations. */
    private EnumerationFactory _enumerationFactory;

    /**
     * Creates a new SourceFactory with the given FieldInfoFactory.
     *
     * @param config the BuilderConfiguration instance (must not be null).
     * @param infoFactory the FieldInfoFactory to use
     * @param groupNaming Group naming scheme to be used.
     */
    public SourceFactory(final BuilderConfiguration config,
            final FieldInfoFactory infoFactory,
            final GroupNaming groupNaming) {
        super(config, infoFactory, groupNaming);

        // set the config into the info factory (CASTOR-1346)
        infoFactory.setBoundProperties(config.boundPropertiesEnabled());

        this.memberFactory       = new MemberFactory(config, infoFactory, _groupNaming);
        this._typeConversion     = new TypeConversion(_config);
        this._enumerationFactory = new EnumerationFactory(_config, _groupNaming);
    } //-- SourceFactory

   /**
     * Sets whether or not to create the XML marshalling framework specific
     * methods (marshall, unmarshall, validate) in the generated classes. By
     * default, these methods are generated.
     *
     * @param createMarshalMethods
     *            a boolean, when true indicates to generated the marshalling
     *            framework methods
     *
     */
    public void setCreateMarshalMethods(final boolean createMarshalMethods) {
        _createMarshalMethods = createMarshalMethods;
    } //-- setCreateMarshalMethpds

    /**
     * Sets whether or not to create extra collection methods for accessing the
     * actual collection.
     *
     * @param extraMethods
     *            a boolean that when true indicates that extra collection
     *            accessor methods should be created. False by default.
     * @see org.exolab.castor.builder.SourceFactory#setReferenceMethodSuffix
     */
    public void setCreateExtraMethods(final boolean extraMethods) {
        _infoFactory.setCreateExtraMethods(extraMethods);
    } //-- setCreateExtraMethods

    /**
     * Sets the method suffix (ending) to use when creating the extra collection
     * methods.
     *
     * @param suffix
     *            the method suffix to use when creating the extra collection
     *            methods. If null or emtpty the default value, as specified in
     *            CollectionInfo will be used.
     * @see org.exolab.castor.builder.SourceFactory#setCreateExtraMethods
     */
    public void setReferenceMethodSuffix(final String suffix) {
        _infoFactory.setReferenceMethodSuffix(suffix);
    } //-- setReferenceMethodSuffix

    /**
     * Sets whether or not to implement CastorTestable.
     *
     * @param testable
     *            if true, indicates to implement CastorTestable
     */
    public void setTestable(final boolean testable) {
           _testable = testable;
    }

   /**
     * Sets to true if SAX1 should be used in the marshall method.
     *
     * @param sax1
     *            true if SAX1 should be used.
     */
    public void setSAX1(final boolean sax1) {
        _sax1 = sax1;
    }

    /**
     * Set to true if enumerated type lookups should be performed in a case
     * insensitive manner.
     *
     * @param caseInsensitive
     *            when true
     */
    public void setCaseInsensitive(final boolean caseInsensitive) {
        _enumerationFactory.setCaseInsensitive(caseInsensitive);
    }

    //------------------/
    //- Public Methods -/
    //------------------/

    /**
     * Creates a new ClassInfo for the given XMLBindingComponent.
     *
     * @param component the XMLBindingComponent that abstracts all XML Schema
     *        definition for a XML Schema component.
     * @param sgState The given state of the SourceGenerator.
     * @return an array of JClasses reflecting the given XMLBindingComponent.
     */
    public JClass[] createSourceCode(final XMLBindingComponent component,
                                     final SGStateInfo sgState) {
        if (component == null) {
            throw new IllegalStateException("XMLBindingComponent may not be null.");
        }
        if (sgState == null) {
            throw new IllegalStateException("SGStateInfo may not be null.");
        }

        //-- check for previous JClass bindings
        JClass[] classes = sgState.getSourceCode(component.getAnnotated());
        if (classes != null) {
            return classes;
        }

        _binding = component.getBinding();

        if (sgState.verbose()) {
             String name = component.getXMLName();
             if (name == null) {
                 name = component.getJavaClassName();
             }
             String msg = "Creating classes for: " + name;
             sgState.getDialog().notify(msg);
        }

        //0-- set the packageName
        String packageName = component.getJavaPackage();
        if (packageName == null || packageName.length() == 0) {
            packageName = sgState._packageName;
        }

        //1-- get the name
        //--if no package used then try to append the default package
        //--used in the SourceGenerator
        String className = component.getQualifiedName();
        if (className.indexOf('.') == -1) {
            //--be sure it is a valid className
            className = JavaNaming.toJavaClassName(className);
            className = resolveClassName(className, packageName);
        }

        //2-- check if we have to create an Item class
        boolean createGroupItem = component.createGroupItem();
        if (createGroupItem) {
            className += ITEM_NAME;
            classes = new JClass[2];
        } else {
            classes = new JClass[1];
        }

        //3-- Create factoryState and chain it to sgState to prevent endless loop
        FactoryState state = new FactoryState(className, sgState, packageName);
        state.setCreateGroupItem(createGroupItem);
        if (sgState.getCurrentFactoryState() != null) {
            state.setParent(sgState.getCurrentFactoryState());
        }
        sgState.setCurrentFactoryState(state);

        //--Prevent endless loop
        if (state.processed(component.getAnnotated())) {
            return new JClass[0];
        }

        //-- Mark the enclosed annotated structure as processed in the
        //-- current FactoryState for preventing endless loop.
        state.markAsProcessed(component.getAnnotated());

        //////////////////////////////////////////////////////
        //NOTE: check that the component is not referring to
        //an imported schema to prevent class creation
        //////////////////////////////////////////////////////

        //4-- intialization of the JClass
        ClassInfo classInfo = state._classInfo;
        JClass    jClass    = state._jClass;
        initialize(jClass);

        //-- name information
        classInfo.setNodeName(component.getXMLName());

        //-- namespace information
        classInfo.setNamespaceURI(component.getTargetNamespace());

        //5--processing the type
        XMLType type = component.getXMLType();
        boolean createForSingleGroup = false;
        boolean creatingForAnElement =
            (component.getAnnotated().getStructureType() == Structure.ELEMENT);

        //-- created from element definition information
        classInfo.setElementDefinition(creatingForAnElement);

        if (type != null) {
            if (type.isComplexType()) {
                processComplexType(component, sgState, state);
            } else if (type.isSimpleType()) {
                SimpleType simpleType = (SimpleType) type;
                //-- handle our special case for enumerated types
                if (simpleType.hasFacet(Facet.ENUMERATION)) {
                    processSimpleTypeEnumeration(component, sgState, classInfo, simpleType);
                } else {
                    //////////////////////////////////////////////////////////
                    //NOTE: generate sources if the flag for generating sources
                    //from imported schemas is on
                    //////////////////////////////////////////////////////////
                    return new JClass[0];
                }
            } else if (type.isAnyType()) {
                //-- Do not create classes for AnyType
                classInfo.setSchemaType(new XSClass(SGTypes.Object));
                return new JClass[0];
            }
        } else {
            //--no type we must be facing an XML schema group
            //--MODEL GROUP OR GROUP
            createForSingleGroup = processSchemaGroup(component, state, classInfo);
        }

        //6--createGroupItem
        if (createGroupItem) {
            //-- create Bound Properties code
            if (component.hasBoundProperties()) {
                createPropertyChangeMethods(jClass);
            }

            sgState.bindReference(jClass, classInfo);

            classes[1] = jClass;

            //-- create main group class
            String fname = component.getJavaClassName() + ITEM_NAME;
            fname = JavaNaming.toJavaMemberName(fname, false);

            FieldInfo fInfo = null;
            if (createForSingleGroup) {
                //By default a nested group Item can occur only once
                fInfo = _infoFactory.createFieldInfo(new XSClass(jClass), fname);
            } else {
                fInfo = _infoFactory.createCollection(
                        new XSClass(jClass), "_items", fname, _config.useJava50());
            }
            fInfo.setContainer(true);
            String newClassName = className.substring(0, className.length() - 4);
            state     = new FactoryState(newClassName, sgState, packageName);
            classInfo = state._classInfo;
            jClass    = state._jClass;
            initialize(jClass);
            if (type != null && type.isComplexType()) {
                ComplexType complexType = (ComplexType) type;
                if (complexType.isTopLevel() ^ creatingForAnElement) {
                    //process attributes and content type since it has not be performed before
                    Annotated saved = component.getAnnotated();
                    processAttributes(component.getBinding(), complexType, state);
                    component.setView(saved);
                    if (complexType.getContentType() == ContentType.mixed) {
                        FieldInfo fieldInfo = memberFactory.createFieldInfoForContent(
                                new XSString(), _config.useJava50());
                        handleField(fieldInfo, state);
                    } else if (complexType.getContentType().getType() == ContentType.SIMPLE) {
                        SimpleContent simpleContent = (SimpleContent) complexType.getContentType();
                        SimpleType temp = simpleContent.getSimpleType();
                        XSType xsType = _typeConversion.convertType(
                                temp, packageName, _config.useJava50());
                        FieldInfo fieldInfo = memberFactory.createFieldInfoForContent(
                                xsType, _config.useJava50());
                        handleField(fieldInfo, state);
                        temp = null;
                    } else {
                        // handle multi-valued choice group
                        classInfo.setSchemaType(new XSClass(jClass));
                    }
                }
            }

            classInfo.addFieldInfo(fInfo);
            fInfo.createJavaField(jClass);
            fInfo.createAccessMethods(jClass, _config.useJava50());
            fInfo.generateInitializerCode(jClass.getConstructor(0).getSourceCode());

            //-- name information
            classInfo.setNodeName(component.getXMLName());

            //-- mark as a container
            classInfo.setContainer(true);
            // -- if we have a superclass, make sure that the actual type extends it, not the
            // xxxItem holder class.
            String actSuperClass = classes[1].getSuperClassQualifiedName();
            jClass.setSuperClass(actSuperClass);
            classes[1].setSuperClass(null);
        }

        classes[0] = jClass;

        //7--set the class information given the component information
        //--base class
        String baseClass = component.getExtends();
        if (baseClass != null && baseClass.length() > 0) {
            //-- at this point if a base class has been set
            //-- it means that it is a class generated for an element
            //-- that extends a class generated for a complexType. Thus
            //-- no change is possible
            if (jClass.getSuperClassQualifiedName() == null) {
                jClass.setSuperClass(baseClass);
            }
        }

        //--interface implemented
        String[] implemented = component.getImplements();
        if (implemented != null) {
            for (int i = 0; i < implemented.length; i++) {
                String interfaceName = implemented[i];
                if ((interfaceName != null) && (interfaceName.length() > 0)) {
                    jClass.addInterface(interfaceName);
                }
            }
        }

        //--final
        jClass.getModifiers().setFinal(component.isFinal());

        //--abstract
        if (component.isAbstract()) {
            jClass.getModifiers().setAbstract(true);
            classInfo.setAbstract(true);
        }

        //-- process annotation
        String comment  = processAnnotations(component.getAnnotated());
        if (comment != null) {
            jClass.getJDocComment().setComment(comment);
        }

        makeMethods(component, sgState, state, jClass, baseClass);

        sgState.bindReference(jClass, classInfo);
        sgState.bindReference(component.getAnnotated(), classInfo);

        //-- Save source code bindings to prevent duplicate code generation
        sgState.bindSourceCode(component.getAnnotated(), classes);

        return classes;
    }

    /**
     * Generate methods for our class.
     * 
     * @param component
     * @param sgState
     * @param state
     * @param jClass
     * @param baseClass
     */
    private void makeMethods(
            final XMLBindingComponent component,
            final SGStateInfo sgState,
            final FactoryState state,
            final JClass jClass,
            final String baseClass) {
        //NOTE: be careful with the derivation stuff when generating bounds properties

        if (_createMarshalMethods) {
            //-- #validate()
            createValidateMethods(jClass);
            //--don't generate marshal/unmarshal methods
            //--for abstract classes
            if (!component.isAbstract()) {
                //-- #marshal()
                createMarshalMethods(jClass);
                //-- #unmarshal()
                createUnmarshalMethods(jClass, sgState);
            }
        }

        //create equals() method?
        if (component.hasEquals()) {
            createEqualsMethod(jClass);
            createHashCodeMethod(jClass);
        }

        //implements CastorTestable?
        if (_testable) {
            createTestableMethods(jClass, state);
        }

        //-- This boolean is set to create bound properties
        //-- even if the user has set the SUPER CLASS property
        String superclassQualifiedName = jClass.getSuperClassQualifiedName();
        if (superclassQualifiedName == null || superclassQualifiedName.equals(baseClass)) {
            //-- create Bound Properties code
            if (component.hasBoundProperties()) {
                createPropertyChangeMethods(jClass);
            }
        }
    }

    private boolean processSchemaGroup(final XMLBindingComponent component,
                                       final FactoryState state, final ClassInfo classInfo) {
        try {
            Group group = (Group) component.getAnnotated();
            processContentModel(group, state);
            component.setView(group);

            //-- Check Group Type
            Order order = group.getOrder();
            if (order == Order.choice) {
                classInfo.getGroupInfo().setAsChoice();
            } else if (order == Order.seq) {
                classInfo.getGroupInfo().setAsSequence();
            } else {
                classInfo.getGroupInfo().setAsAll();
            }

            return group.getMaxOccurs() == 1;
        } catch (ClassCastException ce) {
            //--Should not happen
            throw new IllegalArgumentException("Illegal binding component: " + ce.getMessage());
        }
    }

    private void processSimpleTypeEnumeration(final XMLBindingComponent component,
                                              final SGStateInfo sgState, final ClassInfo classInfo,
                                              final SimpleType simpleType) {
        //-- Don't create source code for simple types that
        //-- don't belong in the elements target namespace
        String tns = simpleType.getSchema().getTargetNamespace();
        boolean create = false;
        if (tns == null) {
            create = (component.getTargetNamespace() == null);
        } else {
            create = tns.equals(component.getTargetNamespace());
        }

        if (create) {
            ClassInfo tmpInfo = sgState.resolve(simpleType);
            JClass tmpClass = null;
            if (tmpInfo != null) {
                tmpClass = tmpInfo.getJClass();
            } else {
                tmpClass = createSourceCode(component.getBinding(), simpleType, sgState);
            }
            classInfo.setSchemaType(new XSClass(tmpClass));
        }
    }

    private void processComplexType(final XMLBindingComponent component, final SGStateInfo sgState,
                                    final FactoryState state) {
        XMLType   type      = component.getXMLType();
        ClassInfo classInfo = state._classInfo;
        JClass    jClass    = state._jClass;
        boolean creatingForAnElement =
            (component.getAnnotated().getStructureType() == Structure.ELEMENT);

        ComplexType complexType = (ComplexType) type;
        if (complexType.isTopLevel() && creatingForAnElement) {
             //--move the view and keep the structure
             Annotated saved = component.getAnnotated();
             String previousPackage = component.getJavaPackage();
             XMLBindingComponent baseComponent = new XMLBindingComponent(_config, _groupNaming);
             baseComponent.setBinding(component.getBinding());
             baseComponent.setView(complexType);
             //-- call createSourceCode to pre-process the complexType
             createSourceCode(baseComponent, sgState);
             String baseClassName = null;
             String basePackage = baseComponent.getJavaPackage();
             //--if the base class is not in the same package
             //--of the current class then we have to qualify the base
             //--class
             if (basePackage != null && !basePackage.equals(previousPackage)) {
                 baseClassName = baseComponent.getQualifiedName();
                 if (baseClassName.indexOf('.') == -1) {
                     //--be sure it is a valid className
                     baseClassName = JavaNaming.toJavaClassName(baseClassName);
                 }
             } else {
                 baseClassName = baseComponent.getJavaClassName();
             }
             jClass.setSuperClass(baseClassName);
             basePackage = null;
             baseClassName = null;
             component.setView(saved);
             saved = null;
        } else if (complexType.isTopLevel() || creatingForAnElement) {
            //generate class if the complexType is anonymous and we create classes
            //for an element OR if the complexType is top-level and we create
            //classes for it.

            //-- check Group type
            if (complexType.getParticleCount() == 1) {
                Particle particle = complexType.getParticle(0);
                if (particle.getStructureType() == Structure.GROUP) {
                    Group group = (Group) particle;
                    if (group.getOrder() == Order.choice) {
                        classInfo.getGroupInfo().setAsChoice();
                    }
                }
            }
            Annotated saved = component.getAnnotated();
            processComplexType(complexType, state);
            component.setView(saved);
            saved = null;
        }
    }

    /**
     * Creates the Java source code to support the given Simpletype.
     *
     * @param binding
     * @param simpleType the Simpletype to create the Java source for
     * @param sgState the current SGStateInfo (cannot be null).
     * @return the JClass representation of the given Simpletype
     */
    public JClass createSourceCode(final ExtendedBinding binding,
            final SimpleType simpleType, final SGStateInfo sgState) {
        if (SimpleTypesFactory.isBuiltInType(simpleType.getTypeCode())) {
            String err = "You cannot construct a ClassInfo for a built-in SimpleType.";
            throw new IllegalArgumentException(err);
        }
        if (sgState == null) {
            throw new IllegalArgumentException("SGStateInfo cannot be null.");
        }

        //-- Unions are currently processed as the built-in
        //-- basetype for the member types of the Union, so
        //-- do nothing for now...however we can warn
        //-- user that no validation will be peformed on the
        //-- union
        if (simpleType.getStructureType() == Structure.UNION) {
            if (!sgState.getSuppressNonFatalWarnings()) {
                String message = "warning: support for unions is incomplete.";
                sgState.getDialog().notify(message);
            }
            return null;
        }

        ClassInfo cInfo = sgState.resolve(simpleType);
        if (cInfo != null) {
            return cInfo.getJClass();
        }

        boolean enumeration = false;

        //-- class name information
        String typeName = simpleType.getName();
        if (typeName == null) {
            Structure struct = simpleType.getParent();
            FactoryState fstate = null;
            switch (struct.getStructureType()) {
                case Structure.ATTRIBUTE:
                    typeName = ((AttributeDecl) struct).getName();
                    fstate = sgState.getCurrentFactoryState();
                    break;
                case Structure.ELEMENT:
                    typeName = ((ElementDecl) struct).getName();
                    break;
                default:
                    // Nothing to do
                    break;
            }
            //-- In case of naming collision we append current class name
            if (fstate != null) {
                typeName = JavaNaming.toJavaClassName(typeName);
                Structure attrDeclParent = ((AttributeDecl) struct).getParent();
                if (attrDeclParent != null
                        && attrDeclParent.getStructureType() == Structure.ATTRIBUTE_GROUP) {
                    typeName = JavaNaming.toJavaClassName(
                            ((AttributeGroupDecl) attrDeclParent).getName() + typeName);
                } else {
                    typeName = fstate._jClass.getLocalName() + typeName;
                }
            }
            //-- otherwise (???) just append "Type"
            typeName += "Type";
        }

        String className   = JavaNaming.toJavaClassName(typeName);

        //--XMLBindingComponent is only used to retrieve the java package
        //-- we need to optimize it by enabling the binding of simpleTypes.
        XMLBindingComponent comp = new XMLBindingComponent(_config, _groupNaming);
        if (binding != null) {
            comp.setBinding(binding);
        }
        comp.setView(simpleType);

        String packageName = comp.getJavaPackage();
        if ((packageName == null) || (packageName.length() == 0)) {
            packageName = sgState._packageName;
        }

        if (simpleType.hasFacet(Facet.ENUMERATION)) {
            enumeration = true;
            //-- Fix packageName TODO: this is a hack I know, we should change this
            if ((packageName != null) && (packageName.length() > 0)) {
                packageName = packageName + "." + SourceGeneratorConstants.TYPES_PACKAGE;
            } else {
                packageName = SourceGeneratorConstants.TYPES_PACKAGE;
            }
        }

        String boundClassName = comp.getJavaClassName();
        if ((boundClassName != null) && (boundClassName.length() > 0)) {
            className = boundClassName;
            typeName = boundClassName;
        }

        className = resolveClassName(className, packageName);

        FactoryState state = new FactoryState(className, sgState, packageName);
        state.setParent(sgState.getCurrentFactoryState());

        ClassInfo classInfo = state._classInfo;
        JClass    jClass    = state._jClass;

        initialize(jClass);

        //-- XML information
        Schema  schema = simpleType.getSchema();
        classInfo.setNamespaceURI(schema.getTargetNamespace());
        classInfo.setNodeName(typeName);

        //-- process annotation
        String comment  = processAnnotations(simpleType);
        if (comment != null) {
            jClass.getJDocComment().setComment(comment);
        }

        XSClass xsClass = new XSClass(jClass, typeName);

        classInfo.setSchemaType(xsClass);

        //-- handle enumerated types
        if (enumeration) {
            xsClass.setAsEnumerated(true);
            processEnumeration(binding, simpleType, state);
        }

        //-- create Bound Properties code
        if (state.hasBoundProperties()) {
            createPropertyChangeMethods(jClass);
        }

        sgState.bindReference(jClass, classInfo);
        sgState.bindReference(simpleType, classInfo);

        return jClass;
    } //-- createSourceCode(SimpleType);

    //-------------------/
    //- Private Methods -/
    //-------------------/

    /**
     * Initializes the given JClass.
     * @param jClass the JClass to initialize
     */
    private void initialize(final JClass jClass) {
        jClass.addInterface("java.io.Serializable");

        if (_config.useJava50()) {
            JAnnotation serial = new JAnnotation(new JAnnotationType("SuppressWarnings"));
            serial.setValue(new String[] {"\"serial\""});
            jClass.addAnnotation(serial);
        }

        //-- add default constructor
        JConstructor con = jClass.createConstructor();
        jClass.addConstructor(con);
        con.getSourceCode().add("super();");
    } //-- initialize

    /**
     * Creates the #marshal methods for the given JClass.
     * @param parent the JClass to create the #marshal methods for
     */
    private void createPropertyChangeMethods(final JClass parent) {
        parent.addImport("java.beans.PropertyChangeEvent");
        parent.addImport("java.beans.PropertyChangeListener");

        //-- add vector to hold listeners
        String vName = "propertyChangeSupport";
        JField field = new JField(SGTypes.PropertyChangeSupport, vName);
        field.getModifiers().makePrivate();
        parent.addField(field);

        //---------------------------------/
        //- notifyPropertyChangeListeners -/
        //---------------------------------/

        JMethod jMethod = new JMethod("notifyPropertyChangeListeners");
        jMethod.getModifiers().makeProtected();

        JDocComment jdc = jMethod.getJDocComment();
        JDocDescriptor jdDesc = null;
        String desc = null;

        desc = "Notifies all registered PropertyChangeListeners "
             + "when a bound property's value changes.";
        jdc.appendComment(desc);

        jMethod.addParameter(new JParameter(SGTypes.String, "fieldName"));
        jdDesc = jdc.getParamDescriptor("fieldName");
        jdDesc.setDescription("the name of the property that has changed.");

        jMethod.addParameter(new JParameter(SGTypes.Object, "oldValue"));
        jdDesc = jdc.getParamDescriptor("oldValue");
        jdDesc.setDescription("the old value of the property.");

        jMethod.addParameter(new JParameter(SGTypes.Object, "newValue"));
        jdDesc = jdc.getParamDescriptor("newValue");
        jdDesc.setDescription("the new value of the property.");

        parent.addMethod(jMethod);
        JSourceCode jsc = jMethod.getSourceCode();
        //--fix for bug 1026
        jsc.add("if (");
        jsc.append(vName);
        jsc.append(" == null) return;");

        jsc.add(vName);
        jsc.append(".firePropertyChange(fieldName,oldValue,newValue);");

        //-----------------------------/
        //- addPropertyChangeListener -/
        //-----------------------------/

        JType jType = new JClass("java.beans.PropertyChangeListener");
        jMethod = new JMethod("addPropertyChangeListener");

        desc = "Registers a PropertyChangeListener with this class.";
        jdc = jMethod.getJDocComment();
        jdc.appendComment(desc);

        jMethod.addParameter(new JParameter(jType, "pcl"));
        desc = "The PropertyChangeListener to register.";
        jdDesc = jdc.getParamDescriptor("pcl");
        jdDesc.setDescription(desc);

        parent.addMethod(jMethod);

        jsc = jMethod.getSourceCode();

        jsc.add("if (");
        jsc.append(vName);
        jsc.append(" == null) {");
        jsc.addIndented(vName + " = new java.beans.PropertyChangeSupport(this);");
        jsc.add("}");
        jsc.add(vName);
        jsc.append(".addPropertyChangeListener(pcl);");

        //--------------------------------/
        //- removePropertyChangeListener -/
        //--------------------------------/

        jMethod = new JMethod("removePropertyChangeListener", JType.BOOLEAN,
                              "always returns true if pcl != null");

        desc = "Removes the given PropertyChangeListener "
             + "from this classes list of ProperyChangeListeners.";
        jdc = jMethod.getJDocComment();
        jdc.appendComment(desc);

        jMethod.addParameter(new JParameter(jType, "pcl"));
        desc = "The PropertyChangeListener to remove.";
        jdDesc = jdc.getParamDescriptor("pcl");
        jdDesc.setDescription(desc);

        parent.addMethod(jMethod);

        jsc = jMethod.getSourceCode();
        jsc.add("if (");
        jsc.append(vName);
        jsc.append(" == null) return false;");

        jsc.add(vName);
        jsc.append(".removePropertyChangeListener(pcl);");
        jsc.add("return true;");
    } //-- createPropertyChangeMethods

    /**
     * Creates the #marshal methods for the given JClass.
     * @param parent the JClass to create the #marshal methods for
     */
    private void createMarshalMethods(final JClass parent) {
        createMarshalMethods(parent, false);
    } //-- createMarshalMethods

    /**
     * Creates the #marshal methods for the given JClass.
     * @param parent the JClass to create the #marshal methods for
     * @param isAbstract true if the generated Class should be marked abstract
     */
    private void createMarshalMethods(final JClass parent, final boolean isAbstract) {
        //-- create main marshal method
        JMethod jMethod = new JMethod("marshal");
        jMethod.addException(SGTypes.MarshalException,
                "if object is null or if any SAXException is thrown during marshaling");
        jMethod.addException(SGTypes.ValidationException,
                "if this object is an invalid instance according to the schema");
        jMethod.addParameter(new JParameter(SGTypes.Writer, "out"));

        //if (_config.useJava50()) {
        // jMethod.addAnnotation(new JAnnotation(new JAnnotationType("Override")));
        //}

        parent.addMethod(jMethod);

        if (isAbstract) {
            jMethod.getModifiers().setAbstract(true);
        } else {
            JSourceCode jsc = jMethod.getSourceCode();
            jsc.add("Marshaller.marshal(this, out);");
        }

        //-- create helper marshal method
        //-- start helper marshal method, this method will
        //-- be built up as we process the given ElementDecl
        jMethod = new JMethod("marshal");
        JClass jc = null;
        if (_sax1) {
            jc = new JClass("org.xml.sax.DocumentHandler");
        } else {
            jc = new JClass("org.xml.sax.ContentHandler");
            jMethod.addException(SGTypes.IOException, "if an IOException occurs during marshaling");
        }
        jMethod.addException(SGTypes.MarshalException,
                "if object is null or if any SAXException is thrown during marshaling");
        jMethod.addException(SGTypes.ValidationException,
                "if this object is an invalid instance according to the schema");
        jMethod.addParameter(new JParameter(jc, "handler"));
        parent.addMethod(jMethod);

        if (isAbstract) {
            jMethod.getModifiers().setAbstract(true);
        } else {
            JSourceCode jsc = jMethod.getSourceCode();
            jsc = jMethod.getSourceCode();
            jsc.add("Marshaller.marshal(this, handler);");
        }

        parent.addImport("org.exolab.castor.xml.Marshaller");
        parent.addImport("org.exolab.castor.xml.Unmarshaller");
    } //-- createMarshalMethods

    private void createUnmarshalMethods(final JClass parent, final SGStateInfo sgState) {
        //-- mangle method name to avoid compiler errors when this class is extended
        String methodName = "unmarshal";
        if (sgState.getSourceGenerator().mappingSchemaType2Java()) {
            methodName += parent.getLocalName();
        }

        //-- create main unmarshal method

        //-- search for proper base class
        JClass returnType = findBaseClass(parent, sgState);
        JMethod jMethod = new JMethod(methodName, returnType,
                                      "the unmarshaled " + returnType.getName());
        jMethod.getModifiers().setStatic(true);
        jMethod.addException(SGTypes.MarshalException,
                "if object is null or if any SAXException is thrown during marshaling");
        jMethod.addException(SGTypes.ValidationException,
                "if this object is an invalid instance according to the schema");
        jMethod.addParameter(new JParameter(SGTypes.Reader, "reader"));
        parent.addMethod(jMethod);

        JSourceCode jsc = jMethod.getSourceCode();
        jsc.add("return (");
        jsc.append(returnType.getName());
        jsc.append(") Unmarshaller.unmarshal(");
        jsc.append(parent.getName());
        jsc.append(".class, reader);");
    } //-- createUnmarshalMethods

    /**
     * Returns the base class (as found in the schema) of the provided class.
     * Climbs the inheritence tree of the provided class to find and return the
     * base class of the provided class.
     *
     * @param jClass class to find the base class of
     * @param sgState current state of source generation
     * @return the base class of the provided class.
     */
    private JClass findBaseClass(final JClass jClass, final SGStateInfo sgState) {
        JClass returnType = jClass;

        List classes = new LinkedList();
        classes.add(returnType);

        while (returnType.getSuperClassQualifiedName() != null) {
            String superClassName = returnType.getSuperClassQualifiedName();
            JClass superClass = sgState.getSourceCode(superClassName);
            if (superClass == null) {
                superClass = sgState.getImportedSourceCode(superClassName);
            }

            // A binding can cause us to have to look for the superclass class in
            // the package of the current class
            if (superClass == null && superClassName.indexOf('.') < 0) {
                String pkgName = returnType.getPackageName();
                if (pkgName != null && pkgName.length() > 0) {
                    superClassName = pkgName + "." + superClassName;
                    superClass = sgState.getSourceCode(superClassName);
                }
            }

            // If returnClass has no superclass then it is the base class
            if (superClass == null) {
                break;
            }

            // Prevent inheritance loops from causing infinite loops
            if (classes.contains(superClass)) {
                StringBuffer buffer = new StringBuffer();
                buffer.append("Loop found in class hierarchy: ");
                for (Iterator i = classes.iterator(); i.hasNext(); ) {
                    JClass element = (JClass) i.next();
                    // If JClass told us the source of the class (ComplexType, Element, ...
                    // then we could report that to and make name conflicts more obvious.
                    buffer.append(element.getName());
                    buffer.append(" -> ");
                }
                buffer.append(superClass.getName());
                sgState.getDialog().notify(buffer.toString());
                // FIXME:  We should probably throw an exception here
                break;
            }

            classes.add(superClass);
            returnType = superClass;
        }

        classes.clear();
        return returnType;
    }

    /**
     * Create an "hashCode" method on the given JClass.
     *
     * @param jclass the JClass in wich we create the hashCode method.
     */
    public static void createHashCodeMethod(final JClass jclass) {
        if (jclass == null) {
            throw new IllegalArgumentException("JClass must not be null");
        }

        // The argument is not null
        JField[] fields = jclass.getFields();

        // Creates the method signature
        JMethod jMethod = new JMethod("hashCode", JType.INT, "a hash code value for the object.");
        jMethod.setComment("Overrides the java.lang.Object.hashCode method.\n"
                           + "<p>\n"
                           + "The following steps came from "
                           + "<b>Effective Java Programming Language Guide</b> "
                           + "by Joshua Bloch, Chapter 3");

        // The hashCode method has no arguments
        jclass.addMethod(jMethod);

        JSourceCode jsc = jMethod.getSourceCode();

        jsc.add("int result = 17;");
        jsc.add("");
        jsc.add("long tmp;");

        for (int i = 0; i < fields.length; i++) {
            JField temp = fields[i];
            // If the field is an object the hashCode method is called recursively

            JType type = temp.getType();
            String name = temp.getName();
            if (type.isPrimitive()) {
                if (type == JType.BOOLEAN) {
                    // Skip the _has_* variables only if they represent
                    // a primitive that may or may not be present
                    if (!name.startsWith("_has_") || jclass.getField(name.substring(5)) != null) {
                        jsc.add("result = 37 * result + (" + name + "?0:1);");
                    }
                } else if (type == JType.BYTE || type == JType.INT || type == JType.SHORT) {
                    jsc.add("result = 37 * result + " + name + ";");
                } else if (type == JType.LONG) {
                    jsc.add("result = 37 * result + (int)(" + name + "^(" + name + ">>>32));");
                } else if (type == JType.FLOAT) {
                    jsc.add("result = 37 * result + java.lang.Float.floatToIntBits(" + name + ");");
                } else if (type == JType.DOUBLE) {
                    jsc.add("tmp = java.lang.Double.doubleToLongBits(" + name + ");");
                    jsc.add("result = 37 * result + (int)(tmp^(tmp>>>32));");
                }
            } else {
                // Calculates hashCode in a recursive manner.
                jsc.add("if (" + name + " != null) {");
                jsc.add("   result = 37 * result + " + name + ".hashCode();");
                jsc.add("}");
            }
        }
        jsc.add("");
        jsc.add("return result;");
    }   //createHashCodeMethod

    /**
     * Create an 'equals' method on the given JClass.
     *
     * @param jclass the Jclass in which we create the equals method
     */
     public void createEqualsMethod(final JClass jclass) {
         if (jclass == null) {
            throw new IllegalArgumentException("JClass must not be null");
         }

        JField[] fields = jclass.getFields();
        JMethod jMethod = new JMethod("equals", JType.BOOLEAN, "true if the objects are equal.");
        jMethod.setComment("Overrides the java.lang.Object.equals method.");
        jMethod.addParameter(new JParameter(SGTypes.Object, "obj"));

        if (_config.useJava50()) {
            jMethod.addAnnotation(new JAnnotation(new JAnnotationType("Override")));
        }

        jclass.addMethod(jMethod);
        JSourceCode jsc = jMethod.getSourceCode();
        jsc.add("if ( this == obj )");
        jsc.indent();
        jsc.add("return true;");
        jsc.unindent();
        if (jclass.getSuperClassQualifiedName() != null) {
            jsc.add("");
            jsc.add("if (super.equals(obj)==false)");
            jsc.indent();
            jsc.add("return false;");
            jsc.unindent();
        }
        jsc.add("");
        jsc.add("if (obj instanceof ");
        jsc.append(jclass.getLocalName());
        jsc.append(") {");
        jsc.add("");
        if (fields.length > 0) {
            jsc.indent();
            jsc.add(jclass.getLocalName());
            jsc.append(" temp = (");
            jsc.append(jclass.getLocalName());
            jsc.append(")obj;");
        }
        for (int i = 0; i < fields.length; i++) {
            JField temp = fields[i];
            //Be careful to arrayList....

            String name = temp.getName();
            if (temp.getType().isPrimitive()) {
                jsc.add("if (this.");
                jsc.append(name);
                jsc.append(" != temp.");
                jsc.append(name);
                jsc.append(")");
            } else {
                //-- Check first if the field is not null. This can occur while comparing
                //-- two objects that contains non-mandatory fields.  We only have to check
                //-- one field since x.equals(null) should return false when equals() is
                //-- correctly implemented.
                jsc.add("if (this.");
                jsc.append(name);
                jsc.append(" != null) {");
                jsc.indent();
                jsc.add("if (temp.");
                jsc.append(name);
                jsc.append(" == null) ");
                jsc.indent();
                jsc.append("return false;");
                jsc.unindent();
                jsc.add("else if (!(");

                // Special handling for comparing arrays
                if (temp.getType().isArray()) {
                    jsc.append("java.util.Arrays.equals(this.");
                    jsc.append(name);
                    jsc.append(", temp.");
                    jsc.append(name);
                    jsc.append(")");
                } else {
                    jsc.append("this.");
                    jsc.append(name);
                    jsc.append(".equals(temp.");
                    jsc.append(name);
                    jsc.append(")");
                }

                jsc.append(")) ");
                jsc.indent();
                jsc.add("return false;");
                jsc.unindent();
                jsc.unindent();
                jsc.add("}"); //end of != null
                jsc.add("else if (temp.");
                jsc.append(name);
                jsc.append(" != null)");
            }
            jsc.indent();
            jsc.add("return false;");
            jsc.unindent();
        }
        jsc.add("return true;");
        jsc.unindent();
        jsc.add("}");
        jsc.add("return false;");
     } //CreateEqualsMethod

    /**
     * Implement org.exolab.castor.tests.framework.CastorTestable im the given JClass.
     *
     * @param jclass The JClass which will implement the CastorTestable Interface.
     * @param state our state, e.g., state of this Factory instance.
     */
     public void createTestableMethods(final JClass jclass, final FactoryState state) {
         if (jclass == null) {
            throw new IllegalArgumentException("JClass must not be null");
         }

        jclass.addInterface("org.exolab.castor.tests.framework.CastorTestable");
        jclass.addImport("org.exolab.castor.tests.framework.CastorTestable");
        jclass.addImport("org.exolab.castor.tests.framework.RandomHelper");

        createRandomizeFields(jclass, state); // implementation of randomizeFields
        createDumpFields(jclass);             // implementation of dumpFields
     } //CreateTestableMethods

     /**
      * Creates the randomizeFields method for a class that implements the
      * interface org.exolab.castor.tests.framework.CastorTestable.
      *
      * @param jclass The JClass which will implement the CastorTestable Interface.
      * @param state
      */
    private void createRandomizeFields(final JClass jclass, final FactoryState state) {
        JMethod jMethod = new JMethod("randomizeFields");
        jMethod.addException(new JClass("InstantiationException"),
                             "if we try to instantiate an abstract class or interface");
        jMethod.addException(new JClass("IllegalAccessException"),
                             "if we do not have access to the field, for example if it is private");
        jMethod.setComment("implementation of org.exolab.castor.tests.framework.CastorTestable");
        jclass.addMethod(jMethod);
        JSourceCode jsc = jMethod.getSourceCode();
        JField[] fields = jclass.getFields();

        for (int i = 0; i < fields.length; i++) {
            JField temp = fields[i];
            JType type = temp.getType();
            String name = temp.getName();

            if (state._fieldInfoForChoice != null
                    && name.equals(state._fieldInfoForChoice.getName())) {
                continue;
            }

            if (name.startsWith("_")) {
                name = JavaNaming.toJavaClassName(name.substring(1));
            } else {
                name = JavaNaming.toJavaClassName(name);
            }

            String setName = "set" + name;
            if (name.indexOf("Has") == -1) {
                if (type instanceof JCollectionType) {
                    //Collection needs a specific handling
                    int listLocat = name.lastIndexOf("List");
                    String tempName = name;
                    if (listLocat != -1) {
                       tempName = tempName.substring(0, listLocat);
                    }
                    String methodName = JavaNaming.toJavaClassName(tempName);
                    methodName = "get" + methodName;
                    JMethod method = jclass.getMethod(methodName, 0);
                    // TODO: handle the Item introduced in with the group handling
                    if (method == null) {
                        continue;
                    }

                    String componentName = method.getReturnType().getName();

                    jsc.add(temp.getName());
                    jsc.append(" = RandomHelper.getRandom(");
                    jsc.append(temp.getName());
                    jsc.append(", ");
                    jsc.append(componentName);
                    jsc.append(".class);");
               } else if (type.isPrimitive()) {
                   // Primitive
                   jsc.add(setName);
                   jsc.append("(RandomHelper.getRandom(");
                   jsc.append(temp.getName());
                   jsc.append("));");
               } else if (type.isArray()) {
                   // Array
                   jsc.add(setName);
                   jsc.append("((");
                   jsc.append(type.toString());
                   jsc.append(")RandomHelper.getRandom(");
                   jsc.append(temp.getName());
                   // Any Class will do, but Array.class seems appropriate
                   jsc.append(", java.lang.reflect.Array.class));");
               } else {
                   // Object
                   jsc.add(setName);
                   jsc.append("((");
                   jsc.append(type.getName());
                   jsc.append(")RandomHelper.getRandom(");
                   jsc.append(temp.getName());
                   jsc.append(", ");
                   jsc.append(type.getName());
                   jsc.append(".class));");
               }
               jsc.add("");
            }
        }
    }

    /**
     * Creates the dumpFields method for a class that implements the interface
     * org.exolab.castor.tests.framework.CastorTestable.
     *
     * @param jclass The JClass which will implement the CastorTestable Interface.
     */
    private void createDumpFields(final JClass jclass) {
        JMethod jMethod = new JMethod("dumpFields", SGTypes.String,
                "a String representation of all of the fields for " + jclass.getName());
        jMethod.setComment("implementation of org.exolab.castor.tests.framework.CastorTestable");
        jclass.addMethod(jMethod);
        JSourceCode jsc = jMethod.getSourceCode();
        jsc.add("StringBuffer result = new StringBuffer(\"DumpFields() for element: ");
        jsc.append(jclass.getName());
        jsc.append("\\n\");");

        JField[] fields = jclass.getFields();
        for (int i = 0; i < fields.length; i++) {
            JField temp = fields[i];
            String name = temp.getName();
            if ((temp.getType().isPrimitive())
                    || temp.getType().getName().startsWith("java.lang.")) {
                //hack when using the option 'primitivetowrapper'
                //this should not interfere with other cases
                jsc.add("result.append(\"Field ");
                jsc.append(name);
                jsc.append(":\" +");
                jsc.append(name);
                jsc.append("+\"\\n\");");
            } else if (temp.getType().isArray()) {
                jsc.add("if (");
                jsc.append(name);
                jsc.append(" != null) {");
                jsc.indent();
                jsc.add("result.append(\"[\");");
                jsc.add("for (int i = 0; i < ");
                jsc.append(name);
                jsc.append(".length; i++) {");
                jsc.indent();
                jsc.add("result.append(");
                jsc.append(name);
                jsc.append("[i] + \" \");");
                jsc.unindent();
                jsc.add("}");
                jsc.add("result.append(\"]\");");
                jsc.unindent();
                jsc.add("}");
            } else {
                jsc.add("if ( (");
                jsc.append(name);
                jsc.append(" != null) && (");
                jsc.append(name);
                jsc.append(".getClass().isAssignableFrom(CastorTestable.class)))");
                jsc.indent();
                jsc.add("result.append(((CastorTestable)");
                jsc.append(name);
                jsc.append(").dumpFields());");
                jsc.unindent();
                jsc.add("else result.append(\"Field ");
                jsc.append(name);
                jsc.append(":\" +");
                jsc.append(name);
                jsc.append("+\"\\n\");");
            }
            jsc.add("");
        }
        jsc.add("");
        jsc.add("return result.toString();");
    }

    /**
     * Creates the Validate methods for the given JClass.
     * @param jClass the JClass to create the Validate methods for
     */
    private void createValidateMethods(final JClass jClass) {
        JMethod     jMethod = null;
        JSourceCode jsc     = null;

        //-- #validate
        jMethod = new JMethod("validate");
        jMethod.addException(SGTypes.ValidationException,
                             "if this object is an invalid instance according to the schema");

        jClass.addMethod(jMethod);
        jsc = jMethod.getSourceCode();
        jsc.add("org.exolab.castor.xml.Validator validator = new ");
        jsc.append("org.exolab.castor.xml.Validator();");
        jsc.add("validator.validate(this);");

        //-- #isValid
        jMethod  = new JMethod("isValid", JType.BOOLEAN,
                               "true if this object is valid according to the schema");
        jsc = jMethod.getSourceCode();
        jsc.add("try {");
        jsc.indent();
        jsc.add("validate();");
        jsc.unindent();
        jsc.add("} catch (org.exolab.castor.xml.ValidationException vex) {");
        jsc.indent();
        jsc.add("return false;");
        jsc.unindent();
        jsc.add("}");
        jsc.add("return true;");
        jClass.addMethod(jMethod);
    } //-- createValidateMethods

    //-------------------/
    //- Private Methods -/
    //-------------------/

    /**
     * Resolves the className out of the given name and the packageName.
     *
     * @param name the class name
     * @param packageName the package name
     * @return the full qualified class name.
     */
    private String resolveClassName(final String name, final String packageName) {
        if ((packageName != null) && (packageName.length() > 0)) {
            return packageName + "." + name;
        }
        return name;
    } //-- resolveClassName

    //////////////////////////////////////////////
    //Process XML Schema structures
    //Note: This code is XML specific, it has to be moved somehow in XMLBindingComponent.
    //The aim of the SourceFactory is to generate code from a BindingComponent.
    ///////////////////////////////////////////////

    /**
     * Creates Comments from Schema annotations.
     * @param annotated the Annotated structure to process
     * @return the generated comment.
     */
    private String processAnnotations(final Annotated annotated) {
        //-- process annotations
        Enumeration enumeration = annotated.getAnnotations();
        if (enumeration.hasMoreElements()) {
            StringBuffer comment = new StringBuffer();
            while (enumeration.hasMoreElements()) {
                Annotation ann = (Annotation) enumeration.nextElement();
                Enumeration documentations = ann.getDocumentation();
                while (documentations.hasMoreElements()) {
                    Documentation documentation = (Documentation) documentations.nextElement();
                    String content = documentation.getContent();
                    if (content != null) {
                        comment.append(content);
                    }
                }
            }
            return normalize(comment.toString());
        }
        return null;
    } //-- processAnnotations

    /**
     * Process the attributes contained in this complexType.
     * @param binding
     * @param complexType the given complex type.
     * @param state the given FactoryState
     */
    private void processAttributes(final ExtendedBinding binding,
            final ComplexType complexType,
            final FactoryState state) {
        if (complexType == null) {
            return;
        }

        Enumeration enumeration = complexType.getAttributeDecls();
        XMLBindingComponent component = new XMLBindingComponent(_config, _groupNaming);
        if (_binding != null) {
            component.setBinding(_binding);
        }

        while (enumeration.hasMoreElements()) {
            AttributeDecl attr = (AttributeDecl) enumeration.nextElement();

            component.setView(attr);

            //-- if we have a new SimpleType...generate ClassInfo
            SimpleType sType = attr.getSimpleType();

            // look for simpleType def in base type(s)
            XMLType baseXMLType = complexType.getBaseType();
            while (sType == null) {
                // If no simple type found: Get the same attribute of the base type.
                // If base type is not complex, forget it; break out of loop now.
                if (baseXMLType == null || !(baseXMLType instanceof ComplexType)) {
                    break;
                }

                // There's a base complexType; get the attribute with the same name
                // as this attribute (=attr) from it
                final ComplexType baseComplexType = (ComplexType) baseXMLType;
                AttributeDecl baseAttribute = baseComplexType.getAttributeDecl(attr.getName());

                if (baseAttribute != null) {
                    // See if this one has a simple-type...
                    sType = baseAttribute.getSimpleType();
                    if (sType != null) {
                        attr.setSimpleType(sType);
                        break;
                    }
                }

                // ... if not, go another step higher in the class hierarchy
                baseXMLType = baseXMLType.getBaseType();
            }

            // Look for referenced type (if any) for setting type, and use
            // it, if found.
            if (sType == null && attr.getReference() != null) {
                attr.setSimpleType(attr.getReference().getSimpleType());
            }

            if (sType != null && !(SimpleTypesFactory.isBuiltInType(sType.getTypeCode()))) {
                if (sType.getSchema() == component.getSchema() && state.resolve(sType) == null) {
                    if (sType.hasFacet(Facet.ENUMERATION)) {
                        createSourceCode(component.getBinding(), sType, state.getSGStateInfo());
                    }
                }
            }

            FieldInfo fieldInfo = memberFactory.createFieldInfo(
                    component, state, _config.useJava50());
            handleField(fieldInfo, state);
        }
    }

    /**
     * @param complexType the ComplexType to process
     * @param state the FactoryState.
     */
    private void processComplexType(final ComplexType complexType, final FactoryState state) {
        XMLBindingComponent component = new XMLBindingComponent(_config, _groupNaming);
        if (_binding != null) {
            component.setBinding(_binding);
        }
        component.setView(complexType);

        String typeName = component.getXMLName();

        ClassInfo classInfo = state._classInfo;
        classInfo.setSchemaType(new XSClass(state._jClass, typeName));

        /// I don't believe this should be here: kv 20030423
        ///classInfo.setNamespaceURI(component.getTargetNamespace());

        //- Handle derived types
        XMLType base = complexType.getBaseType();

        //-- if the base is a complexType, we need to process it
        if (base != null) {
            if (base.isComplexType()) {
                String baseClassName = null;

                component.setView(base);
                //-- Is this base type from the schema we are currently generating source for?
                //////////////////////////////////////////////////////////
                //NOTE: generate sources if the flag for generating sources
                //from imported schemas in on
                //////////////////////////////////////////////////////////
                if (base.getSchema() == complexType.getSchema()) {
                    ClassInfo cInfo = state.resolve(base);
                    //--no classInfo yet so no source code available
                    //--for the base type: we need to generate it
                    if (cInfo == null) {
                        JClass[] classes = createSourceCode(component, state.getSGStateInfo());
                        cInfo = state.resolve(base);
                        baseClassName = classes[0].getName();
                    } else {
                        baseClassName = cInfo.getJClass().getName();
                    }
                    //set the base class
                    classInfo.setBaseClass(cInfo);
                } else {
                    //-- Create qualified class name for a base type class
                    //-- from another package
                    baseClassName = component.getQualifiedName();
                 }
                //-- Set super class
                //-- and reset the view on the current ComplexType
                component.setView(complexType);
                // only set a super class name if the current complexType is not a
                // restriction of a simpleContent (--> no object hierarchy, only content hierarchy)
                /*
                 Note: There are times when a simpleContent restriction needs to
                 extend the hierarchy, such as a restriction of a restriction, so
                 I'm commenting out the following line for now. see bug 1875
                 for more details. If this causes any regressions we'll need to
                 find a more appropriate solution.
                 if (! ( complexType.isRestricted() && ((ComplexType)base).isSimpleContent() ) )
                */
                state._jClass.setSuperClass(baseClassName);
            } //--complexType

            //--if the content type is a simpleType create a field info for it.
            if (complexType.getContentType().getType() == ContentType.SIMPLE) {
                SimpleContent simpleContent = (SimpleContent) complexType.getContentType();
                SimpleType temp = simpleContent.getSimpleType();
                SimpleType baseType = (SimpleType) temp.getBaseType();
                XSType xsType = _typeConversion.convertType(
                        temp, state._packageName, _config.useJava50());

                FieldInfo fieldInfo = null;
                if ((baseType != null) && extendsSimpleType(state._jClass, baseType, state)) {
                    if (xsType.isEnumerated()) {
                        fieldInfo = memberFactory.createFieldInfoForContent(
                                xsType, _config.useJava50());
                        fieldInfo.setBound(false);
                        handleField(fieldInfo, state);

                        //-- remove getter since we don't need to override the original getter
                        String mname = fieldInfo.getReadMethodName();
                        JClass jClass = state._jClass;
                        JMethod method = jClass.getMethod(mname, 0);
                        jClass.removeMethod(method);

                        //-- update setter method
                        mname = fieldInfo.getWriteMethodName();
                        method = jClass.getMethod(mname, 0);
                        JSourceCode jsc = method.getSourceCode();
                        jsc.add("super.");
                        jsc.append(mname);
                        jsc.append("(this.");
                        jsc.append(fieldInfo.getName());
                        jsc.append(".toString());");
                    }
                    //-- else just use superclass setters/getters
                    //-- do nothing
                } else {
                    while (temp.getBaseType() != null) {
                        temp = (SimpleType) temp.getBaseType();
                    }
                    xsType = _typeConversion.convertType(
                            temp, state._packageName, _config.useJava50());
                    fieldInfo = memberFactory.createFieldInfoForContent(
                            xsType, _config.useJava50());
                    handleField(fieldInfo, state);
                }
            }
        } //--base not null

        //---------------------/
        //- handle attributes -/
        //- and mixed content -/
        //---------------------/

        if (!state.isCreateGroupItem()) {
            processAttributes(component.getBinding(), complexType, state);
            //--reset the view on the current ComplexType
            component.setView(complexType);
            if (complexType.getContentType() == ContentType.mixed) {
                FieldInfo fieldInfo = memberFactory.createFieldInfoForContent(
                        new XSString(), _config.useJava50());
                handleField(fieldInfo, state);
            }
        }
        //--process the contentModelGroup
        processContentModel(complexType, state);
    } //-- processComplextype

    /**
     * Processes the given ContentModelGroup. This method is responsible for
     * creating FieldInfos (or sometimes ClassInfos) for elements and
     * model group contained in the given ContentModelGroup.
     *
     * @param contentModel the ContentModelGroup to process
     * @param state the current FactoryState.
     */
    private void processContentModel(final ContentModelGroup model, final FactoryState state) {
        //------------------------------/
        //- handle elements and groups -/
        //------------------------------/

        ContentModelGroup contentModel = model;
        Enumeration enumeration = contentModel.enumerate();

        //-- handle choice item
        if (state._classInfo.isChoice() && state._fieldInfoForChoice == null) {
            state._fieldInfoForChoice = memberFactory.createFieldInfoForChoiceValue();
            state._fieldInfoForChoice.createJavaField(state._jClass);
            state._fieldInfoForChoice.createAccessMethods(state._jClass, _config.useJava50());
        }

        FieldInfo fieldInfo = null;
        XMLBindingComponent component = new XMLBindingComponent(_config, _groupNaming);
        if (_binding != null) {
            component.setBinding(_binding);
        }

        while (enumeration.hasMoreElements()) {
            Annotated annotated = (Annotated) enumeration.nextElement();
            component.setView(annotated);

            switch(annotated.getStructureType()) {
                case Structure.ELEMENT: //-- handle element declarations
                    fieldInfo = memberFactory.createFieldInfo(
                            component, state, _config.useJava50());
                    //-- Fix for element declarations being used in
                    //-- a group with minOccurs = 0;
                    //-- (kvisco - 20021007)
                    if (contentModel.getMinOccurs() == 0) {
                        fieldInfo.setRequired(false);
                    }
                    handleField(fieldInfo, state);
                    break;
                case Structure.GROUP: //-- handle groups
                    Group group = (Group) annotated;
                    //set the compositor
                    if ((contentModel instanceof ComplexType)
                            || (contentModel instanceof ModelGroup)) {
                        if (group.getOrder() == Order.choice) {
                            state._classInfo.getGroupInfo().setAsChoice();
                        } else if (group.getOrder() == Order.all) {
                            state._classInfo.getGroupInfo().setAsAll();
                        } else if (group.getOrder() == Order.seq) {
                            state._classInfo.getGroupInfo().setAsSequence();
                        }
                    }

                    //-- create class member,if necessary
                    if (!((contentModel instanceof ComplexType)
                            || (contentModel instanceof ModelGroup))) {
                        if (contentModel instanceof ModelGroup) {
                            ModelGroup mg = (ModelGroup) contentModel;
                            if (mg.isReference()) {
                                contentModel = mg.getReference();
                            }
                        }

                        if (contentModel.getParticleCount() > 0) {
                            fieldInfo = memberFactory.createFieldInfo(component,
                                                                       state.getSGStateInfo(),
                                                                       _config.useJava50());
                            handleField(fieldInfo, state);
                        }
                    } else {
                       //--else we just flatten the group
                       processContentModel(group, state);
                    }
                    break;

                case Structure.MODELGROUP:
                     ModelGroup modelgroup = (ModelGroup) annotated;
                     //--a Model Group definition can only referenced
                     //--another group at this point.
                     //get the contentModel and proccess it
                    if (modelgroup.getName() != null) {
                        //create the field info for the element
                        //that is referring to a model group in order
                        //not to loose the Particle information
                        if (modelgroup.isReference()) {
                            modelgroup = modelgroup.getReference();
                        }

                        if (modelgroup.getParticleCount() > 0) {
                            fieldInfo = memberFactory.createFieldInfo(
                                    component, state.getSGStateInfo(), _config.useJava50());
                            handleField(fieldInfo, state);
                        }
                        break;
                    }
                    //--else we just flatten the group
                    processContentModel(modelgroup.getContentModelGroup(), state);
                    break;

                case Structure.WILDCARD:
                    Wildcard wildcard = (Wildcard) annotated;
                    FieldInfo fieldForAny = memberFactory.createFieldInfoForAny(
                            wildcard, _config.useJava50());
                    handleField(fieldForAny, state);
                    break;

                default:
                    break;
            }
        }

    } //-- process(ContentModelGroup)

    /**
     * Creates all the necessary enumeration code from the given Creates all the
     * necessary enumeration code from the given SimpleType. Enumerations are
     * handled a couple ways.
     *
     * @param binding
     * @param simpleType the SimpleType we are processing an enumeration for
     * @param state our current state
     * @see #processEnumerationAsBaseType
     */
    private void processEnumeration(final ExtendedBinding binding,
            final SimpleType simpleType, final FactoryState state) {
        // Added by robertlaferla at comcast dot net 01/21/2004
        if (_config.useEnumeratedTypeInterface()) {
            state._jClass.addImport(ENUM_ACCESS_INTERFACE);
            state._jClass.addInterface(ENUM_ACCESS_INTERFACE);
        } // end enumTypeInterface

        switch (enumerationType) {
            case BASE_TYPE_ENUMERATION:
                processEnumerationAsBaseType(binding, simpleType, state);
                break;
            default:
                processEnumerationAsNewObject(binding, simpleType, state);
                break;
        }
    } //-- processEnumeration

    /**
     * Creates all the necessary enumeration code from the given SimpleType. Delegates
     * to EnumerationFactory.
     *
     * @param binding
     * @param simpleType the SimpleType we are processing an enumeration for
     * @param state our current state
     * @see #processEnumerationAsBaseType
     */
    private void processEnumerationAsNewObject(final ExtendedBinding binding,
            final SimpleType simpleType, final FactoryState state) {
        _enumerationFactory.processEnumerationAsNewObject(binding, simpleType, state);
    } //-- processEnumerationAsNewObject

    /**
     * Delegates creation of enumeration code to EnumerationFactory.
     *
     * @param binding
     * @param simpleType the SimpleType we are processing an enumeration for
     * @param state our current state
     */
    private void processEnumerationAsBaseType(final ExtendedBinding binding,
            final SimpleType simpleType, final FactoryState state) {
        _enumerationFactory.processEnumerationAsBaseType(binding, simpleType, state);
    } //-- processEnumerationAsBaseType

    /**
     * Adds a given FieldInfo to the JClass and ClassInfo stored in the given
     * FactoryState.
     *
     * @param fieldInfo The fieldInfo to add
     * @param state the current FactoryState
     */
    private void handleField(final FieldInfo fieldInfo, final FactoryState state) {
        if (fieldInfo == null) {
            return;
        }

        if (CLASS_METHOD_SUFFIX.equals(fieldInfo.getMethodSuffix())) {
            SGStateInfo sInfo = state.getSGStateInfo();
            if (!sInfo.getSuppressNonFatalWarnings()) {
                String warn = "warning a field name conflicts with \""
                    + CLASS_KEYWORD + "\", please use a binding file to specify "
                    + "a different name for the " + fieldInfo.getNodeTypeName()
                    + " '" + fieldInfo.getNodeName() + "'.";
                sInfo.getDialog().notify(warn);
            }
        } else if (CLASS_KEYWORD.equals(fieldInfo.getNodeName())) {
            SGStateInfo sInfo = state.getSGStateInfo();
            if (!sInfo.getSuppressNonFatalWarnings()) {
                String warn = "warning a field name conflicts with \""
                    + CLASS_KEYWORD + "\" and is being replaced by \"clazz\". "
                    + "You may use a binding file to specify a different "
                    + "name for the " + fieldInfo.getNodeTypeName()
                    + " '" + fieldInfo.getNodeName() + "'.";
                sInfo.getDialog().notify(warn);
            }
        }

        JSourceCode scInitializer = state._jClass.getConstructor(0).getSourceCode();

        ClassInfo base = state._classInfo.getBaseClass();
        boolean present = false;
        if (base != null) {
            switch (fieldInfo.getNodeType()) {
                case XMLInfo.ATTRIBUTE_TYPE:
                    present = (base.getAttributeField(fieldInfo.getNodeName()) != null);
                    break;
                case XMLInfo.ELEMENT_TYPE:
                    String baseNodeName = fieldInfo.getNodeName();
                    // TODO[WG]: replace this eror check with something more meaningful
                    if (!(baseNodeName.equals(XMLInfo.CHOICE_NODE_NAME_ERROR_INDICATION))) {
                        present = (base.getElementField(baseNodeName) != null);
                    }
                    break;
                default:
                    break;
            }
        }

        state._classInfo.addFieldInfo(fieldInfo);
        present = present && !fieldInfo.isMultivalued();
        //create the relevant Java fields only if the field
        //info is not yet in the base classInfo or if it is not a collection
        if (!present) {
            if (state._fieldInfoForChoice != null) {
                if (fieldInfo != state._fieldInfoForChoice) {
                    fieldInfo.setFieldInfoReference(state._fieldInfoForChoice);
                }
            }

            fieldInfo.createJavaField(state._jClass);
            //-- do not create access methods for transient fields
            if (!fieldInfo.isTransient()) {
                fieldInfo.createAccessMethods(state._jClass, _config.useJava50());
                if (fieldInfo.isBound()) {
                    state.setBoundProperties(true);
                }
            }
        }

        //-- Add initialization code
        fieldInfo.generateInitializerCode(scInitializer);
    } //-- handleField

    /**
     * Returns true if the given JClass extends the class represented by the
     * given SimpleType.
     *
     * @param jClass the JClass to check
     * @param type the SimpleType to check against
     * @param state the FactoryState
     *
     * @return true if the given JClass extends the class associated with the
     *         given SimpleType, otherwise false.
     */
    private boolean extendsSimpleType(final JClass jClass, final SimpleType type,
                                      final FactoryState state) {
        String superClassName = jClass.getSuperClassQualifiedName();
        if (superClassName != null) {
            ClassInfo cInfo = state.resolve(type);
            if (cInfo != null) {
               return superClassName.equals(cInfo.getJClass().getName());
            }
        }
        return false;
    } //-- extendsSimpleType

} //-- SourceFactory
