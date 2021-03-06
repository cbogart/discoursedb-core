/*******************************************************************************
 * Copyright (C)  2015 - 2016  Carnegie Mellon University
 * Author: Oliver Ferschke
 *
 * This file is part of DiscourseDB.
 *
 * DiscourseDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * DiscourseDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DiscourseDB.  If not, see <http://www.gnu.org/licenses/> 
 * or write to the Free Software Foundation, Inc., 51 Franklin Street, 
 * Fifth Floor, Boston, MA 02110-1301  USA
 *******************************************************************************/
package edu.cmu.cs.lti.discoursedb.core.repository.macro;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.cmu.cs.lti.discoursedb.core.model.macro.DiscoursePart;
import edu.cmu.cs.lti.discoursedb.core.model.macro.DiscoursePartRelation;
import edu.cmu.cs.lti.discoursedb.core.repository.BaseRepository;

public interface DiscoursePartRelationRepository extends BaseRepository<DiscoursePartRelation, Long> {
	Optional<DiscoursePartRelation> findOneBySourceAndTargetAndType(DiscoursePart source, DiscoursePart Target, String type);
	List<DiscoursePartRelation> findAllBySourceAndType(DiscoursePart source, String type);
	Page<DiscoursePartRelation> findAllBySource(DiscoursePart source, Pageable page);
	
	@Query("select targ from DiscoursePartRelation dpr left join dpr.target targ left join dpr.source src where src=:source order by targ.startTime")
	Page<DiscoursePart> findAllTargetsBySource(@Param("source") DiscoursePart source, Pageable page);
	
	List<DiscoursePartRelation> findAllBySource(DiscoursePart source);
	Page<DiscoursePartRelation> findAllByTarget(DiscoursePart discoursePart, Pageable page);	


}
