/*
 * Copyright 2007 Ralf Joachim
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
package org.castor.cpa.persistence.convertor;

/**
 * Convert <code>org.exolab.castor.types.Date</code> to <code>java.sql.Date</code>.
 * 
 * @author <a href="mailto:ralf DOT joachim AT syscon DOT eu">Ralf Joachim</a>
 * @version $Revision: 7134 $ $Date: 2006-04-25 15:08:23 -0600 (Tue, 25 Apr 2006) $
 * @since 1.1.3
 */
public final class CastorDateToSqlDate extends AbstractSimpleTypeConvertor {
    //-----------------------------------------------------------------------------------

    /**
     * Default constructor.
     */
    public CastorDateToSqlDate() {
        super(org.exolab.castor.types.Date.class, java.sql.Date.class);
    }

    //-----------------------------------------------------------------------------------
    
    /**
     * {@inheritDoc}
     */
    public Object convert(final Object object) {
        long time = ((org.exolab.castor.types.Date) object).toDate().getTime();
        return new java.sql.Date(time);
    }

    //-----------------------------------------------------------------------------------
}