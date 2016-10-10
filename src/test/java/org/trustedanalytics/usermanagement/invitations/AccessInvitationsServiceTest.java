/**
 *  Copyright (c) 2015 Intel Corporation 
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.trustedanalytics.usermanagement.invitations;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.trustedanalytics.usermanagement.invitations.service.AccessInvitations;
import org.trustedanalytics.usermanagement.invitations.service.AccessInvitationsService;
import org.trustedanalytics.usermanagement.storage.KeyValueStore;

import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class AccessInvitationsServiceTest {

    private static final String USER_EMAIL = "email@example.com";

    private AccessInvitationsService sut;

    @Mock
    private AccessInvitations mockUserInvitations;

    @Mock
    private KeyValueStore<AccessInvitations> mockInvitationsStore;

    @Before
    public void setUp() {
        sut = new AccessInvitationsService(mockInvitationsStore);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAccessInvitations_emptyEmailGiven_throwIllegalArgument() {
        sut.getAccessInvitations("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateOrUpdateInvitation_emptyEmail_throwIllegalArgument() {
        Consumer consumer = mock(Consumer.class);
        sut.createOrUpdateInvitation("", consumer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateOrUpdateInvitation_nullEmail_throwIllegalArgument() {
        Consumer consumer = mock(Consumer.class);
        sut.createOrUpdateInvitation(null, consumer);
    }

    @Test
    public void testCreateOrUpdateInvitation_invitationDoesNotExists_createsNew() {
        when(mockInvitationsStore.hasKey(USER_EMAIL)).thenReturn(false);
        Consumer consumer = mock(Consumer.class);
        AccessInvitationsService.CreateOrUpdateState state = sut.createOrUpdateInvitation(USER_EMAIL, consumer);

        verify(consumer).accept(any());
        verify(mockInvitationsStore, never()).get(anyString());
        assertEquals(AccessInvitationsService.CreateOrUpdateState.CREATED, state);
    }

    @Test
    public void testCreateOrUpdateInvitation_invitationExists_replace() {
        when(mockInvitationsStore.hasKey(USER_EMAIL)).thenReturn(true);
        Consumer consumer = mock(Consumer.class);
        AccessInvitationsService.CreateOrUpdateState state = sut.createOrUpdateInvitation(USER_EMAIL, consumer);

        verify(consumer).accept(any());
        verify(mockInvitationsStore).get(anyString());
        assertEquals(AccessInvitationsService.CreateOrUpdateState.UPDATED, state);

    }



}
